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
import at.roteskreuz.stopcorona.model.entities.session.*
import at.roteskreuz.stopcorona.model.entities.session.ProcessingPhase.DailyBatch
import at.roteskreuz.stopcorona.model.entities.session.ProcessingPhase.FullBatch
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.workers.DelayedExposureBroadcastReceiverCallWorker
import at.roteskreuz.stopcorona.model.workers.DownloadInfectionMessagesWorker
import at.roteskreuz.stopcorona.model.workers.ExposureMatchingWorker
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeBoolean
import at.roteskreuz.stopcorona.utils.endOfTheUtcDay
import at.roteskreuz.stopcorona.utils.extractLatestRedAndYellowContactDate
import at.roteskreuz.stopcorona.utils.minusDays
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
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
    private val preferences: SharedPreferences,
    private val quarantineRepository: QuarantineRepository,
    private val workManager: WorkManager,
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val configurationRepository: ConfigurationRepository
) : InfectionMessengerRepository,
    CoroutineScope {

    companion object {
        private const val PREF_SOMEONE_HAS_RECOVERED = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "someone_has_recovered"
    }

    private val downloadMessagesStateObserver = StateObserver()

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

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

        if (configuration.scheduledProcessingIn5Min) {
            if (sessionDao.deleteScheduledSession(token) == 0) {
                Timber.d("ENStatusUpdates: Proceessing of $token was already triggered")
                return
            } else {
                Timber.d("ENStatusUpdates: Processing token $token")
            }
        }

        val fullSession = sessionDao.getFullSession(token) ?: run {
            Timber.e(SilentError(IllegalStateException("ENStatusUpdates: Session for token $token not found")))
            return
        }

        // To be safe (form exceptions) assume processing is finished until it is overwritten below
        var processingFinished = true
        try {
            processingFinished = when (fullSession.session.processingPhase) {
                FullBatch -> {
                    Timber.d("Let´s evaluate the fullbatch based on the summary and the current warning state")
                    processResultsOfFullBatch(configuration, fullSession)
                }
                DailyBatch -> {
                    Timber.d("Let´s evaluate the next daily batch based on the summary and the current warning state ")
                    processResultsOfNextDailyBatch(configuration, fullSession)
                }
            }
        } finally {
            if (processingFinished) {
                // No further processing has been scheduled.
                cleanUpSession(fullSession)
            }
        }
    }

    private suspend fun cleanUpSession(fullSession: DbFullSession) {
        exposureNotificationRepository.removeDiagnosisKeyBatchParts(fullSession.fullBatchParts)
        exposureNotificationRepository.removeDiagnosisKeyBatchParts(fullSession.dailyBatchesParts)
        sessionDao.deleteSession(fullSession.session)
    }

    private suspend fun processResultsOfNextDailyBatch(
        configuration: DbConfiguration,
        fullSession: DbFullSession
    ): Boolean {
        val exposureInformation =
            exposureNotificationRepository.getExposureInformationWithPotentiallyInformingTheUser(fullSession.session.currentToken)

        val dates = exposureInformation.extractLatestRedAndYellowContactDate(configuration.dailyRiskThreshold)

        val firstRedDay = dates.firstRedDay
        val firstYellowDay = fullSession.session.firstYellowDay ?: dates.firstYellowDay.also { Timber.e("Yellow warning found") }
        sessionDao.updateSession(fullSession.session.copy(firstYellowDay = dates.firstYellowDay))

        // Found red warning? Done processing.
        firstRedDay?.let { _ ->
            firstYellowDay?.let { _ ->
                quarantineRepository.receivedWarning(WarningType.YELLOW, timeOfContact = firstYellowDay)
            } ?: quarantineRepository.revokeLastYellowContactDate()

            quarantineRepository.receivedWarning(WarningType.RED, timeOfContact = firstRedDay)
            Timber.e("Red warning found. Done processing.")
            return true // Processing done
        }

        val remainingDailyBatchesParts = fullSession.remainingDailyBatchesParts
        // End of batch without red warning? Done processing.
        if (remainingDailyBatchesParts.isEmpty()) {
            quarantineRepository.revokeLastRedContactDate()
            firstYellowDay?.let { _ ->
                quarantineRepository.receivedWarning(WarningType.YELLOW, timeOfContact = firstYellowDay)
                Timber.e("Done processing. Only Yellow warning(s) found")
            } ?: run {
                Timber.e("Done processing. No warnings found")
                quarantineRepository.revokeLastYellowContactDate()
            }
            return true // Processing done
        }

        return processAndDropNextDayPersistState(remainingDailyBatchesParts, fullSession)
    }

    private suspend fun processResultsOfFullBatch(
        configuration: DbConfiguration,
        fullSession: DbFullSession
    ): Boolean {
        val currentWarningType = fullSession.session.warningType
        val token = fullSession.session.currentToken
        val summary = exposureNotificationRepository.determineRiskWithoutInformingUser(token)

        when (currentWarningType) {
            WarningType.YELLOW, WarningType.RED -> {
                if (summary.summationRiskScore < configuration.dailyRiskThreshold) {
                    quarantineRepository.revokeLastRedContactDate()
                    quarantineRepository.revokeLastYellowContactDate()
                    return true // Processing done
                } else {
                    val exposureInformations = exposureNotificationRepository.getExposureInformationWithPotentiallyInformingTheUser(token)

                    val dates = exposureInformations.extractLatestRedAndYellowContactDate(configuration.dailyRiskThreshold)

                    dates.firstRedDay?.let {
                        quarantineRepository.receivedWarning(WarningType.RED, dates.firstRedDay)
                    }
                    dates.firstYellowDay?.let {
                        quarantineRepository.receivedWarning(WarningType.YELLOW, dates.firstYellowDay)
                    }
                    // Only revoce quarantines after all new quarantines are known.
                    // When switching from yellow to red, if we revoke yellow above imediately, the red quarantine is not yet known and the
                    // quarantine end tile is triggered in the ui
                    if (dates.firstRedDay == null) quarantineRepository.revokeLastRedContactDate()
                    if (dates.firstYellowDay == null) quarantineRepository.revokeLastYellowContactDate()

                    return true // Processing done
                }
            }
            WarningType.GREEN -> {
                //we are above risc for the last days!!!
                if (summary.summationRiskScore >= configuration.dailyRiskThreshold) {

                    //1. let´s remove batches we definitly don´t need
                    val relevantDailyBatchesParts = fullSession.dailyBatchesParts.filter { dailyBatchPart ->
                        // [   ][   ][ 2 ][   ][now]
                        // xxxxxxxxxxxxxxx|<- this is the reference date for minusDays(2)
                        // we want all the xxxxxxxxxxxxxxx
                        val referenceDate = Instant.now().minusDays(summary.daysSinceLastExposure.toLong()).endOfTheUtcDay()
                        dailyBatchPart.intervalStart < referenceDate.epochSecond
                    }
                    Timber.d("filtered the relevantDailyBatchesParts to length ${relevantDailyBatchesParts.size} ")
                    return processAndDropNextDayPersistState(relevantDailyBatchesParts, fullSession)
                } else {
                    Timber.d("We are still WarningType.GREEN")
                    return true // Processing done
                }
            }
        }
    }

    /**
     * we find the batch files of the next day, process them and drop them from the database
     *
     * @return True if processing has finished. False if more batches are expected to come
     */
    private suspend fun processAndDropNextDayPersistState(
        relevantDailyBatchParts: List<DbDailyBatchPart>,
        fullSession: DbFullSession
    ): Boolean {
        val listOfDailyBatchParts = relevantDailyBatchParts
            .groupBy { dailyBatchPart ->
                dailyBatchPart.intervalStart
            }.toSortedMap().map { (_, dailyBatchParts) ->
                dailyBatchParts
            }

        if (listOfDailyBatchParts.isEmpty()) {
            Timber.e(
                SilentError(java.lang.IllegalStateException("processAndDropNextDayPersistState should not be called with empty list of batches")))
            return true // Processing done
        }

        val batchToProcess = listOfDailyBatchParts.first()
        sessionDao.updateDailyBatchParts(batchToProcess.map { it.copy(processed = true) })

        val newToken = UUID.randomUUID().toString()
        sessionDao.updateSession(fullSession.session.copy(
            currentToken = newToken,
            processingPhase = DailyBatch
        ))
        Timber.d("Processing the next day files: ${batchToProcess.joinToString(",") { it.fileName }} ")
        return startDiagnosisKeyMatching(batchToProcess, newToken)
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

                sessionDao.insertFullSession(fullSession)
                startDiagnosisKeyMatching(fullBatchParts, token)
            } catch (e: Exception) {
                Timber.e(e, "Downloading new diagnosis keys failed")
                downloadMessagesStateObserver.error(e)
            } finally {
                downloadMessagesStateObserver.idle()
            }
        }
    }

    private suspend fun startDiagnosisKeyMatching(
        fullBatchParts: List<DbBatchPart>,
        token: String
    ): Boolean {
        Timber.d("ENStatusUpdates: Providing diagnosis batch. Token: $token")
        val finished = exposureNotificationRepository.provideDiagnosisKeyBatch(fullBatchParts, token)

        // schedule calling [ExposureNotificationBroadcastReceiver.onExposureStateUpdated] after timeout
        if (!finished && configurationRepository.getConfiguration()?.scheduledProcessingIn5Min != false) {
            Timber.d("ENStatusUpdates: Scheduling a timeout for the exposure status update broadcast. Token: $token")
            sessionDao.insertScheduledSession(DbScheduledSession(token))
            DelayedExposureBroadcastReceiverCallWorker.enqueueDelayedExposureReceiverCall(workManager, token)
        }

        return finished
    }

    private fun ApiIndexOfDiagnosisKeysArchives.fullBatchForWarningType(warningType: WarningType): ApiDiagnosisKeysBatch {
        return when (warningType) {
            WarningType.YELLOW, WarningType.RED -> full14DaysBatch
            WarningType.GREEN -> full07DaysBatch
        }
    }

    private suspend fun fetchDailyBatchesDiagnosisKeys(dailyBatches: List<ApiDiagnosisKeysBatch>)
        : List<DbDailyBatchPart> {
        return coroutineScope {
            dailyBatches.flatMap { dailyBatch ->
                dailyBatch.batchFilePaths.mapIndexed { index, path ->
                    async {
                        DbDailyBatchPart(
                            batchNumber = index,
                            intervalStart = dailyBatch.intervalToEpochSeconds,
                            fileName = apiInteractor.downloadContentDeliveryFile(path)
                        )
                    }
                }.map {
                    it.await()
                }
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