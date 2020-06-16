package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.db.dao.SessionDao
import at.roteskreuz.stopcorona.model.db.dao.TemporaryExposureKeysDao
import at.roteskreuz.stopcorona.model.entities.exposure.DbSentTemporaryExposureKeys
import at.roteskreuz.stopcorona.model.entities.infection.exposure_keys.ApiDiagnosisKeysBatch
import at.roteskreuz.stopcorona.model.entities.infection.exposure_keys.ApiIndexOfDiagnosisKeysArchives
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.entities.session.DbDailyBatchPart
import at.roteskreuz.stopcorona.model.entities.session.DbFullBatchPart
import at.roteskreuz.stopcorona.model.entities.session.DbFullSession
import at.roteskreuz.stopcorona.model.entities.session.DbSession
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
import at.roteskreuz.stopcorona.skeleton.core.utils.stringSharedPreferencesProperty
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
    private val cryptoRepository: CryptoRepository,
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
        private const val PREF_LAST_SCHEDULED_TOKEN = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "last_scheduled_token"
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

    private var lastScheduledToken: String by preferences.stringSharedPreferencesProperty(PREF_LAST_SCHEDULED_TOKEN, "no token ever")

    override fun enqueueDownloadingNewMessages() {
        DownloadInfectionMessagesWorker.enqueueDownloadInfection(workManager)
    }

    override suspend fun storeSentTemporaryExposureKeys(temporaryExposureKeys: List<TemporaryExposureKeysWrapper>) {
        temporaryExposureKeysDao.insertSentTemporaryExposureKeys(temporaryExposureKeys)
    }

    override suspend fun processKeysBasedOnToken(token: String) {
        //we´re processing the batches
        if (lastScheduledToken != token) {
            Timber.e(IllegalArgumentException("processing of token ${token} when we expect to process ${lastScheduledToken}"))
        }
        val configuration =
            configurationRepository.getConfiguration()
                ?: throw IllegalStateException("we have no configuration values here, it doesn´t make sense to continue")

        //TODO: delete the old diagnosis key files as they have been processed
        val fullSession: DbFullSession = sessionDao.getFullSession(token)
        val warningType = WarningType.valueOf(fullSession.session.warningType)

        val summary = exposureNotificationRepository.determineRiskWithoutInformingUser(token)
        when (warningType) {
            WarningType.YELLOW, WarningType.RED -> {
                //TODO finish this decision branch
                if (summary.summationRiskScore >= configuration.dailyRiskThreshold) {
                    val summary = exposureNotificationRepository.getExposureSummaryWithPotentiallyInformingTheUser(token)
                    //go through the days and check if the day is the first RED/YELLOW day
                    // TODO: go through the summary and check if the day is the first RED/YELLOW day
                } else {
                    when (warningType) {
                        WarningType.RED -> quarantineRepository.revokeLastRedContactDate()
                        WarningType.YELLOW -> quarantineRepository.revokeLastYellowContactDate()
                    }
                    quarantineRepository.setShowQuarantineEnd()
                }
            }
            WarningType.GREEN -> {
                //we are above risc for the last days!!!
                if (summary.summationRiskScore >= configuration.dailyRiskThreshold) {
                    //we must now identify day by day if we are YELLOW or RED
                    //val listOfDaysWithDownloadedFilesSortedByServer = apiInteractor.fetchDailyBatchDiagnosisKeys()
                    //process the batches and do one of these:
                    //TODO find time of contact
                    //this is a fake calculation:
                    val dayOfExposure = ZonedDateTime.now().minusDays(summary.daysSinceLastExposure.toLong())
                    quarantineRepository.receivedWarning(WarningType.YELLOW, timeOfContact = dayOfExposure)
                    var quarantineStatus = quarantineRepository.getQuarantineStatus()
                } else {
                    Timber.d("We are still WarningType.REVOKE")
                }
            }
        }
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
                val contextToken = UUID.randomUUID().toString()
                val index = apiInteractor.getIndexOfDiagnosisKeysArchives()
                val fullBatchParts = fetchFullBatchDiagnosisKeys(index.fullBatchForWarningType(warningType))
                val dailyBatchesParts = fetchDailyBatchesDiagnosisKeys(index.dailyBatches)

                val fullSession = DbFullSession(
                    session = DbSession(
                        token = contextToken,
                        warningType = warningType.name
                    ),
                    fullBatchParts = fullBatchParts,
                    dailyBatchesParts = dailyBatchesParts
                )

                sessionDao.insertOrUpdateFullSession(fullSession)
                lastScheduledToken = contextToken
                exposureNotificationRepository.processBatchDiagnosisKeys(fullBatchParts, contextToken)
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

    private suspend fun fetchDailyBatchesDiagnosisKeys(dailyBatches: List<ApiDiagnosisKeysBatch>): List<DbDailyBatchPart> {
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