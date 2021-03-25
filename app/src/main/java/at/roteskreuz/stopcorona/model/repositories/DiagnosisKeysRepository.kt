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
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.skeleton.core.utils.*
import at.roteskreuz.stopcorona.utils.endOfTheUtcDay
import at.roteskreuz.stopcorona.utils.extractLatestRedAndYellowContactDate
import at.roteskreuz.stopcorona.utils.minusDays
import at.roteskreuz.stopcorona.utils.shareReplayLast
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing diagnosis keys and infection related user status
 */
interface DiagnosisKeysRepository {

    /**
     * Store to DB the sent temporary exposure keys.
     */
    suspend fun storeSentTemporaryExposureKeys(temporaryExposureKeys: List<TekMetadata>)

    /**
     * Start a process of downloading diagnosis keys (previously known as infection messages)
     */
    suspend fun fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()

    /**
     * Get the sent temporary exposure keys.
     */
    suspend fun getSentTeksByMessageType(messageType: MessageType): List<DbSentTemporaryExposureKeys>

    /**
     * Get all the sent temporary exposure keys.
     */
    suspend fun getSentTemporaryExposureKeys(): List<DbSentTemporaryExposureKeys>

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
     * Fetch [com.google.android.gms.nearby.exposurenotification.ExposureSummary]  and
     * [com.google.android.gms.nearby.exposurenotification.ExposureInformation] based on the token
     * and the user health status and process the information to a exposure health status which
     * will be displayed in the UI.
     */
    suspend fun processKeysBasedOnToken(token: String)

    /**
     * Observe date of the last key request.
     */
    fun observeDateOfLastKeyRequest(): Observable<Optional<ZonedDateTime>>

    /**
     * Returns the number of key requests for the last 7 days.
     */
    fun getKeyRequestCountLastWeek(): Int?

    /**
     * Adds the date of last key request and the number for updating the dashboard key request data.
     */
    fun addAndUpdateKeyRequestData()

}

class DiagnosisKeysRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val sessionDao: SessionDao,
    private val temporaryExposureKeysDao: TemporaryExposureKeysDao,
    private val preferences: SharedPreferences,
    private val quarantineRepository: QuarantineRepository,
    private val workManager: WorkManager,
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val configurationRepository: ConfigurationRepository,
    val notificationsRepository: NotificationsRepository,
    private val isGMS : Boolean
) : DiagnosisKeysRepository,
    CoroutineScope {

    companion object {
        private const val PREF_SOMEONE_HAS_RECOVERED = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "someone_has_recovered"
        private const val PREF_DATE_OF_LAST_KEY_REQUEST = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "date_of_last_key_request"
        private const val PREF_DATES_OF_KEY_REQUESTS = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "count_of_key_requests"
    }

    private val downloadMessagesStateObserver = StateObserver()

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    private var someoneHasRecovered: Boolean by preferences.booleanSharedPreferencesProperty(
        PREF_SOMEONE_HAS_RECOVERED,
        false
    )

    private var dateOfLastKeyRequest: ZonedDateTime? by preferences.nullableZonedDateTimeSharedPreferencesProperty(
        PREF_DATE_OF_LAST_KEY_REQUEST
    )

    private var dateOfLastKeyRequestObservable = preferences.observeNullableZonedDateTime(
        PREF_DATE_OF_LAST_KEY_REQUEST
    ).shareReplayLast()

    override suspend fun storeSentTemporaryExposureKeys(temporaryExposureKeys: List<TekMetadata>) {
        temporaryExposureKeysDao.insertSentTemporaryExposureKeys(temporaryExposureKeys)
    }

    override suspend fun processKeysBasedOnToken(token: String) {
        val configuration = configurationRepository.getConfiguration() ?: run {
            Timber.e(SilentError(IllegalStateException("no configuration present, failing silently")))
            return
        }

        if (isGMS && configuration.scheduledProcessingIn5Min) {
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

    override fun observeDateOfLastKeyRequest(): Observable<Optional<ZonedDateTime>> {
        return dateOfLastKeyRequestObservable
    }

    override fun getKeyRequestCountLastWeek(): Int? {
        val datesOfKeyRequests = preferences.getStringSet(PREF_DATES_OF_KEY_REQUESTS, mutableSetOf())
        return datesOfKeyRequests?.size
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

        val lastRedDay = dates.lastRedDay
        val lastYellowDay = fullSession.session.firstYellowDay ?: dates.lastYellowDay.also { Timber.e("Yellow warning found") }
        sessionDao.updateSession(fullSession.session.copy(firstYellowDay = dates.lastYellowDay))

        // Found red warning? Done processing.
        lastRedDay?.let { _ ->
            lastYellowDay?.let { _ ->
                quarantineRepository.receivedWarning(WarningType.YELLOW, timeOfContact = lastYellowDay)
            }

            quarantineRepository.receivedWarning(WarningType.RED, timeOfContact = lastRedDay)

            // Only revoke quarantines after all new quarantines are known.
            // When switching from yellow to red, if we revoke yellow above imediately, the red quarantine is not yet known and the
            // quarantine end tile is triggered in the ui
            if (dates.lastYellowDay == null) {
                quarantineRepository.revokeLastYellowContactDate()
            }

            Timber.e("Red warning found. Done processing.")
            return true // Processing done
        }

        val remainingDailyBatchesParts = fullSession.remainingDailyBatchesParts
        // End of batch without red warning? Done processing.
        if (remainingDailyBatchesParts.isEmpty()) {
            lastYellowDay?.let { _ ->
                quarantineRepository.receivedWarning(WarningType.YELLOW, timeOfContact = lastYellowDay)
                Timber.e("Done processing. Only Yellow warning(s) found")
            } ?: run {
                Timber.e("Done processing. No warnings found")
                quarantineRepository.revokeLastYellowContactDate()
            }
            quarantineRepository.revokeLastRedContactDate()
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
            WarningType.GREEN, // No special handling of WarningType.GREEN as the optimization is currently broken. See comment on `else` branch.
            WarningType.YELLOW, WarningType.RED -> {
                if (summary.summationRiskScore < configuration.dailyRiskThreshold) {
                    if (quarantineRepository.dateOfLastRedContact != null || quarantineRepository.dateOfLastYellowContact != null){
                        notificationsRepository.displayEndQuarantineNotification()
                    }
                    quarantineRepository.revokeLastRedContactDate()
                    quarantineRepository.revokeLastYellowContactDate()
                    return true // Processing done
                } else {
                    val exposureInformations = exposureNotificationRepository.getExposureInformationWithPotentiallyInformingTheUser(token)

                    val dates = exposureInformations.extractLatestRedAndYellowContactDate(configuration.dailyRiskThreshold)

                    dates.lastRedDay?.let {
                        quarantineRepository.receivedWarning(WarningType.RED, dates.lastRedDay)
                    }
                    dates.lastYellowDay?.let {
                        quarantineRepository.receivedWarning(WarningType.YELLOW, dates.lastYellowDay)
                    }
                    // Only revoce quarantines after all new quarantines are known.
                    // When switching from yellow to red, if we revoke yellow above imediately, the red quarantine is not yet known and the
                    // quarantine end tile is triggered in the ui
                    if (dates.lastRedDay == null) quarantineRepository.revokeLastRedContactDate()
                    if (dates.lastYellowDay == null) quarantineRepository.revokeLastYellowContactDate()

                    if (currentWarningType == WarningType.GREEN && dates.noDates()) {
                        notificationsRepository.displayNotificationForLowRisc()
                    }

                    return true // Processing done
                }
            }
            // This branch is disabled (i.e. will never be reached). It is an optimized handling of WarningType.GREEN which is currently broken
            // due to a bug in Google's EN framework which drops broadcasts if no keys matched at all.
            else -> {
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
                val fetchDaily = when (warningType) {
                    WarningType.GREEN, // No special handling of WarningType.GREEN as the optimization is currently broken. See comment on `else` branch.
                    WarningType.YELLOW, WarningType.RED -> false
                    // This branch is disabled (i.e. will never be reached). It is an optimized handling of WarningType.GREEN which is currently broken
                    // due to a bug in Google's EN framework which drops broadcasts if no keys matched at all.
                    else -> true
                }
                val token = UUID.randomUUID().toString()
                val index = apiInteractor.getIndexOfDiagnosisKeysArchives()
                val fullBatchParts = fetchFullBatchDiagnosisKeys(index.fullBatchForWarningType(warningType))
                val dailyBatchesParts = if (fetchDaily) fetchDailyBatchesDiagnosisKeys(index.dailyBatches) else emptyList()

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

                addAndUpdateKeyRequestData()

                startDiagnosisKeyMatching(fullBatchParts, token)
            } catch (e: Exception) {
                Timber.e(e, "Downloading new diagnosis keys failed")
                downloadMessagesStateObserver.error(e)
            } finally {
                downloadMessagesStateObserver.idle()
            }
        }
    }

    override fun addAndUpdateKeyRequestData() {
        // Updated the last key request date and the number of key requests in the last week for the dashboard
        val datesOfKeyRequest = preferences.getStringSet(PREF_DATES_OF_KEY_REQUESTS, mutableSetOf())

        datesOfKeyRequest?.let {
            val itr = datesOfKeyRequest.iterator()

            while (itr.hasNext()) {
                val date = itr.next()
                val time = Instant.ofEpochMilli(date.toLong())
                if (time.isBefore(Instant.now().minusDays(7))) {
                    itr.remove()
                }
            }
            datesOfKeyRequest.add(Instant.now().toEpochMilli().toString())
            preferences.putAndApply(PREF_DATES_OF_KEY_REQUESTS, datesOfKeyRequest)
        }

        dateOfLastKeyRequest = ZonedDateTime.now()
    }

    private suspend fun startDiagnosisKeyMatching(
        fullBatchParts: List<DbBatchPart>,
        token: String
    ): Boolean {
        Timber.d("ENStatusUpdates: Providing diagnosis batch. Token: $token")
        val finished = exposureNotificationRepository.provideDiagnosisKeyBatch(fullBatchParts, token)

        // schedule calling [ExposureNotificationBroadcastReceiver.onExposureStateUpdated] after timeout
        if (isGMS && !finished && configurationRepository.getConfiguration()?.scheduledProcessingIn5Min != false) {
            Timber.d("ENStatusUpdates: Scheduling a timeout for the exposure status update broadcast. Token: $token")
            sessionDao.insertScheduledSession(DbScheduledSession(token))
            DelayedExposureBroadcastReceiverCallWorker.enqueueDelayedExposureReceiverCall(workManager, token)
        }

        return finished
    }

    private fun ApiIndexOfDiagnosisKeysArchives.fullBatchForWarningType(warningType: WarningType): ApiDiagnosisKeysBatch {
        return when (warningType) {
            WarningType.GREEN, // No special handling of WarningType.GREEN as the optimization is currently broken. See comment on `else` branch.
            WarningType.YELLOW, WarningType.RED -> full14DaysBatch
            // This branch is disabled (i.e. will never be reached). It is an optimized handling of WarningType.GREEN which is currently broken
            // due to a bug in Google's EN framework which drops broadcasts if no keys matched at all.
            else -> full07DaysBatch
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

    override suspend fun getSentTeksByMessageType(messageType: MessageType): List<DbSentTemporaryExposureKeys> {
        return temporaryExposureKeysDao.getSentTemporaryExposureKeysByMessageType(messageType)
    }

    override suspend fun getSentTemporaryExposureKeys(): List<DbSentTemporaryExposureKeys> {
        return temporaryExposureKeysDao.getSentTemporaryExposureKeys()
    }

    override fun setSomeoneHasRecovered() {
        someoneHasRecovered = true
    }

    override fun someoneHasRecoveredMessageSeen() {
        someoneHasRecovered = false
    }
}

/**
 * Describes a temporary exposure key, it's associated random password and messageType.
 */
data class TekMetadata(
    val validity: Validity,
    val password: UUID,
    val messageType: MessageType
)
