package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.db.dao.SessionDao
import at.roteskreuz.stopcorona.model.db.dao.TemporaryExposureKeysDao
import at.roteskreuz.stopcorona.model.entities.configuration.DbConfiguration
import at.roteskreuz.stopcorona.model.entities.exposure.DbSentTemporaryExposureKeys
import at.roteskreuz.stopcorona.model.entities.infection.exposure_keys.ApiDiagnosisKeysBatch
import at.roteskreuz.stopcorona.model.entities.infection.exposure_keys.ApiIndexOfDiagnosisKeysArchives
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.entities.session.DbDailyBatchPart
import at.roteskreuz.stopcorona.model.entities.session.DbFullBatchPart
import at.roteskreuz.stopcorona.model.entities.session.DbFullSession
import at.roteskreuz.stopcorona.model.entities.session.DbSession
import at.roteskreuz.stopcorona.model.entities.session.ProcessingPhase.DailyBatch
import at.roteskreuz.stopcorona.model.entities.session.ProcessingPhase.FullBatch
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.managers.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.workers.DownloadInfectionMessagesWorker
import at.roteskreuz.stopcorona.model.workers.ExposureMatchingWorker
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.nullableLongSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeBoolean
import at.roteskreuz.stopcorona.utils.endOfTheDay
import at.roteskreuz.stopcorona.utils.extractLatestRedAndYellowContactDate
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing infection messages.
 */
interface InfectionMessengerRepository {

    /**
     * Enqueue download and processing infection messages.
     */
    fun enqueueDownloadingNewMessages()

    /**
     * Store to DB the sent temporary exposure keys.
     */
    suspend fun storeSentTemporaryExposureKeys(temporaryExposureKeys: List<TemporaryExposureKeysWrapper>)

    /**
     * Start a process of downloading diagnosis keys (previously known as infection messages)
     */
    suspend fun fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()

    /**
     * Get the sent temporary exposure keys.
     */
    suspend fun getSentTemporaryExposureKeysByMessageType(messageType: MessageType): List<DbSentTemporaryExposureKeys>

    /**
     * Observe info if someone has recovered.
     * To hide it user must call [someoneHasRecoveredMessageSeen].
     */
    fun observeSomeoneHasRecoveredMessage(): Observable<Boolean>

    /**
     * Show the message someone has recovered.
     */
    fun setSomeoneHasRecovered()

    /**
     * Hide the message someone has recovered.
     */
    fun someoneHasRecoveredMessageSeen()

    /**
     * Enqueue the next work request to run the exposure matching algorithm.
     */
    fun enqueueNextExposureMatching()

    /**
     * Fetch [com.google.android.gms.nearby.exposurenotification.ExposureSummary]  and
     * [com.google.android.gms.nearby.exposurenotification.ExposureInformation] based on the token
     * and the user health status and process the information to a exposure health status which
     * will be displayed in the UI.
     */
    suspend fun processKeysBasedOnToken(token: String)
}

class InfectionMessengerRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val sessionDao: SessionDao,
    private val temporaryExposureKeysDao: TemporaryExposureKeysDao,
    private val notificationsRepository: NotificationsRepository,
    private val preferences: SharedPreferences,
    private val quarantineRepository: QuarantineRepository,
    private val workManager: WorkManager,
    private val databaseCleanupManager: DatabaseCleanupManager,
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val configurationRepository: ConfigurationRepository
) : InfectionMessengerRepository,
    CoroutineScope {

    companion object {
        private const val PREF_LAST_MESSAGE_ID = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "last_message_id"
        private const val PREF_SOMEONE_HAS_RECOVERED = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "someone_has_recovered"
    }

    private val downloadMessagesStateObserver = StateObserver()

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    /**
     * Stores and provides last and biggest infection message id.
     */
    private var lastMessageId: Long? by preferences.nullableLongSharedPreferencesProperty(
        PREF_LAST_MESSAGE_ID
    )

    private var someoneHasRecovered: Boolean by preferences.booleanSharedPreferencesProperty(
        PREF_SOMEONE_HAS_RECOVERED,
        false
    )

    override fun enqueueDownloadingNewMessages() {
        DownloadInfectionMessagesWorker.enqueueDownloadInfection(workManager)
    }

    override suspend fun storeSentTemporaryExposureKeys(temporaryExposureKeys: List<TemporaryExposureKeysWrapper>) {
        temporaryExposureKeysDao.insertSentTemporaryExposureKeys(temporaryExposureKeys)
    }

    override suspend fun processKeysBasedOnToken(token: String) {
        val configuration = configurationRepository.getConfiguration() ?: run {
            Timber.e(SilentError(IllegalStateException("no configuration present, failing silently")))
            return
        }

        val fullSession = sessionDao.getFullSession(token) ?: run {
            Timber.e(SilentError(IllegalStateException("Session for token ${token} not found")))
            return
        }

        //TODO: CTAA-1664 cleanup the files and sessionDao (delete by token)

        when (fullSession.session.processingPhase) {
            FullBatch -> {
                Timber.d("Let´s evaluate based on the summary and WarningType the fullbatch")
                processResultsOfFullBatch(token, configuration, fullSession)
            }
            DailyBatch -> {
                Timber.d("Let´s evaluate based on the summary and WarningType the next daily batch")
                processResultsOfNextDailyBatch(fullSession, configuration)
            }
        }
    }

    private suspend fun InfectionMessengerRepositoryImpl.processResultsOfNextDailyBatch(
        fullSession: DbFullSession,
        configuration: DbConfiguration) {
        val exposureInformation =
            exposureNotificationRepository.getExposureInformationWithPotentiallyInformingTheUser(fullSession.session.currentToken)

        val dates = exposureInformation.extractLatestRedAndYellowContactDate(configuration.dailyRiskThreshold)

        if (dates.firstYellowDay != null && fullSession.session.firstYellowDay == null) {
            fullSession.session.firstYellowDay = dates.firstYellowDay
        }

        dates.firstRedDay?.let { firstRedDay ->
            fullSession.session.firstYellowDay?.let { firstYellowDay ->
                quarantineRepository.receivedWarning(WarningType.YELLOW, timeOfContact = firstYellowDay)
            }

            quarantineRepository.receivedWarning(WarningType.RED, timeOfContact = dates.firstRedDay)
            return
        }

        if (fullSession.dailyBatchesParts.isEmpty()) {
            val firstYellowDay = fullSession.session.firstYellowDay
            if (firstYellowDay != null) {
                quarantineRepository.receivedWarning(WarningType.YELLOW, timeOfContact = firstYellowDay)
            } else {
                Timber.e("we´re done processing but it seems there is no yellow date")
            }
        } else {
            processAndDropNextDayPersistState(fullSession.dailyBatchesParts, fullSession)
        }
    }

    private suspend fun processResultsOfFullBatch(token: String,
        configuration: DbConfiguration,
        fullSession: DbFullSession) {
        val currentWarningType = fullSession.session.warningType
        val summary = exposureNotificationRepository.determineRiskWithoutInformingUser(token)

        when (currentWarningType) {
            WarningType.YELLOW, WarningType.RED -> {
                if (summary.summationRiskScore < configuration.dailyRiskThreshold) {
                    when (currentWarningType) {
                        WarningType.RED -> quarantineRepository.revokeLastRedContactDate()
                        WarningType.YELLOW -> quarantineRepository.revokeLastYellowContactDate()
                    }
                } else {
                    val exposureInformations = exposureNotificationRepository.getExposureInformationWithPotentiallyInformingTheUser(token)

                    val dates = exposureInformations.extractLatestRedAndYellowContactDate(configuration.dailyRiskThreshold)

                    dates.firstRedDay?.let { quarantineRepository.receivedWarning(WarningType.RED, dates.firstRedDay) }
                    dates.firstYellowDay?.let { quarantineRepository.receivedWarning(WarningType.YELLOW, dates.firstYellowDay) }
                }
            }
            WarningType.GREEN -> {
                //we are above risc for the last days!!!
                if (summary.summationRiskScore >= configuration.dailyRiskThreshold) {

                    //1. let´s remove batches we definitly don´t need
                    val relevantDailyBatchesParts = fullSession.dailyBatchesParts.filter {
                        // [   ][   ][ 2 ][   ][now]
                        // xxxxxxxxxxxxxxx|<- this is the reference date for minusDays(2)
                        // we want all the xxxxxxxxxxxxxxx
                        val referenceDate = ZonedDateTime.now().minusDays(summary.daysSinceLastExposure.toLong()).endOfTheDay()
                        it.intervalStart < referenceDate.toEpochSecond()

                        //org.threeten.bp.ZonedDateTime.ofInstant(org.threeten.bp.Instant.ofEpochSecond(fullSession.dailyBatchesParts.get(0).intervalStart), ZoneId.systemDefault())
                    }
                    Timber.d("filtered the relevantDailyBatchesParts to length ${relevantDailyBatchesParts.size} ")
                    processAndDropNextDayPersistState(relevantDailyBatchesParts, fullSession)
                } else {
                    Timber.d("We are still WarningType.REVOKE")
                }
            }

        }
    }

    /**
     * we find the batch files of the next day, process them and drop them from the database
     */
    private suspend fun processAndDropNextDayPersistState(
        relevantDailyBatchesParts: List<DbDailyBatchPart>,
        fullSession: DbFullSession) {
        val listOfDailyBatchParts = relevantDailyBatchesParts
            .groupBy { dailyBatchPart ->
                dailyBatchPart.intervalStart
            }.toSortedMap().map { (intervalStart, dailyBatchParts) ->
                dailyBatchParts
            }

        val batchToProcess = listOfDailyBatchParts.first()
        val remainingBatches = listOfDailyBatchParts.drop(1)
        fullSession.dailyBatchesParts = remainingBatches.flatten()

        val newToken = UUID.randomUUID().toString()
        fullSession.session.currentToken = newToken
        fullSession.session.processingPhase = DailyBatch
        sessionDao.insertOrUpdateFullSession(fullSession)
        Timber.d("Processing the next day files: ${batchToProcess.map { it.fileName }.joinToString(",")} ")
        exposureNotificationRepository.processDiagnosisKeyBatch(batchToProcess, newToken)
    }

    override suspend fun fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework() {
        if (downloadMessagesStateObserver.currentState is State.Loading) {
            Timber.e(SilentError(IllegalStateException("we´re trying to download but we´re still downloading...")))
            return
        }

        downloadMessagesStateObserver.loading()
        withContext(coroutineContext) {
            try {
                val warningType = quarantineRepository.getCurrentWarningType()
                val token = UUID.randomUUID().toString()
                val index = apiInteractor.getIndexOfDiagnosisKeysArchives()
                val fullBatchParts = fetchFullBatchDiagnosisKeys(index.fullBatchForWarningType(warningType))
                val dailyBatchesParts = fetchDailyBatchesDiagnosisKeys(index.dailyBatches)

                val fullSession = DbFullSession(
                    session = DbSession(
                        currentToken = token,
                        warningType = warningType,
                        processingPhase = FullBatch,
                        firstYellowDay = null
                    ),
                    fullBatchParts = fullBatchParts,
                    dailyBatchesParts = dailyBatchesParts
                )

                sessionDao.insertOrUpdateFullSession(fullSession)
                exposureNotificationRepository.processDiagnosisKeyBatch(fullBatchParts, token)
            } catch (e: Exception) {
                Timber.e(e, "Downloading new diagnosis keys failed")
                downloadMessagesStateObserver.error(e)
            } finally {
                downloadMessagesStateObserver.idle()
            }
        }
    }

    private fun ApiIndexOfDiagnosisKeysArchives.fullBatchForWarningType(warningType: WarningType): ApiDiagnosisKeysBatch {
        return when (warningType) {
            WarningType.YELLOW, WarningType.RED -> full14DaysBatch
            WarningType.GREEN -> full07DaysBatch
        }
    }

    private suspend fun fetchDailyBatchesDiagnosisKeys(dailyBatches: List<ApiDiagnosisKeysBatch>)
        : List<DbDailyBatchPart> {
        return dailyBatches.flatMap { dailyBatch ->
            dailyBatch.batchFilePaths.mapIndexed { index, path ->
                DbDailyBatchPart(
                    batchNumber = index,
                    intervalStart = dailyBatch.intervalToEpochSeconds,
                    fileName = apiInteractor.downloadContentDeliveryFile(path)
                )
            }
        }
    }

    private suspend fun fetchFullBatchDiagnosisKeys(batch: ApiDiagnosisKeysBatch): List<DbFullBatchPart> {
        return batch.batchFilePaths.mapIndexed { index, path ->
            DbFullBatchPart(
                batchNumber = index,
                intervalStart = batch.intervalToEpochSeconds,
                fileName = apiInteractor.downloadContentDeliveryFile(path)
            )
        }
    }

    override fun observeSomeoneHasRecoveredMessage(): Observable<Boolean> {
        return preferences.observeBoolean(PREF_SOMEONE_HAS_RECOVERED, false)
    }

    override suspend fun getSentTemporaryExposureKeysByMessageType(messageType: MessageType): List<DbSentTemporaryExposureKeys> {
        return temporaryExposureKeysDao.getSentTemporaryExposureKeysByMessageType(messageType)
    }

    override fun setSomeoneHasRecovered() {
        someoneHasRecovered = true
    }

    override fun someoneHasRecoveredMessageSeen() {
        someoneHasRecovered = false
    }

    override fun enqueueNextExposureMatching() {
        ExposureMatchingWorker.enqueueNextExposureMatching(workManager)
    }
}

/**
 * Describes a temporary exposure key, it's associated random password and messageType.
 */
data class TemporaryExposureKeysWrapper(
    val rollingStartIntervalNumber: Int,
    val password: UUID,
    val messageType: MessageType
)