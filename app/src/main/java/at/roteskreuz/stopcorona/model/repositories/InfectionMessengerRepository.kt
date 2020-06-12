package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.db.dao.InfectionMessageDao
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.message.DbReceivedInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.DbSentInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.InfectionMessageContent
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
import at.roteskreuz.stopcorona.utils.asDbObservable
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
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
     * Store to DB sent messages and contacts.
     */
    suspend fun storeSentInfectionMessages(messages: List<Pair<ByteArray, InfectionMessageContent>>)

    /**
     * Start a process of downloading diagnosis keys (previously known as infection messages)
     */
    suspend fun fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()

    /**
     * Observe the infection messages received.
     */
    fun observeReceivedInfectionMessages(): Observable<List<DbReceivedInfectionMessage>>

    /**
     * Observe the infection messages sent.
     */
    fun observeSentInfectionMessages(): Observable<List<DbSentInfectionMessage>>

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
    private val infectionMessageDao: InfectionMessageDao,
    private val cryptoRepository: CryptoRepository,
    private val notificationsRepository: NotificationsRepository,
    private val preferences: SharedPreferences,
    private val quarantineRepository: QuarantineRepository,
    private val workManager: WorkManager,
    private val databaseCleanupManager: DatabaseCleanupManager,
    private val exposureNotificationRepository: ExposureNotificationRepository
) : InfectionMessengerRepository,
    CoroutineScope {

    companion object {
        private const val PREF_LAST_MESSAGE_ID = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "last_message_id"
        private const val PREF_SOMEONE_HAS_RECOVERED = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "someone_has_recovered"
        private const val PREF_LAST_SCHEDULED_TOKEN = Constants.Prefs.INFECTION_MESSENGER_REPOSITORY_PREFIX + "last_scheduled_token"

        private const val BATCH = "batch_"
    }

    private val downloadMessagesStateObserver = StateObserver()

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    /**
     * Stores and provides last and biggest infection message id.
     */
    private var lastMessageId: Long? by preferences.nullableLongSharedPreferencesProperty(PREF_LAST_MESSAGE_ID)

    private var someoneHasRecovered: Boolean by preferences.booleanSharedPreferencesProperty(PREF_SOMEONE_HAS_RECOVERED, false)

    private var lastScheduledToken: String by preferences.stringSharedPreferencesProperty(PREF_LAST_SCHEDULED_TOKEN, "no token ever")

    override fun enqueueDownloadingNewMessages() {
        DownloadInfectionMessagesWorker.enqueueDownloadInfection(workManager)
    }

    override suspend fun storeSentInfectionMessages(messages: List<Pair<ByteArray, InfectionMessageContent>>) {
        infectionMessageDao.insertSentInfectionMessages(messages)
    }

    override suspend fun processKeysBasedOnToken(token: String){
        //we´re processing the batches
        if (lastScheduledToken != token){
            Timber.e(IllegalArgumentException("processing of token ${token} when we expect to process ${lastScheduledToken}"))
        }

        if (token.startsWith(BATCH)){
            processKeysBasedOnBatchToken(token)
        } else {
            processDailyTokens(token)
        }
    }

    private fun processDailyTokens(token: String) {
        //we can assume you are WarningType.GREEN as only in this case we process the tokens
    }

    private suspend fun processKeysBasedOnBatchToken(token: String) {
        val warningType = WarningType.RED
        when(warningType) {
            WarningType.YELLOW, WarningType.RED -> {

            }
            WarningType.REVOKE -> {
                val summary = exposureNotificationRepository.determineRiskWithoutInformingUser(token)
                summary.summationRiskScore
            }
        }
    }

    override suspend fun fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework() {
        val warningType = WarningType.RED
        if (downloadMessagesStateObserver.currentState is State.Loading) {
            Timber.e(SilentError(IllegalStateException("we´re trying to download but we´re still downloading...")))
            return
        }

        downloadMessagesStateObserver.loading()
        withContext(coroutineContext) {
            try {
                val archives :List<File> = apiInteractor.fetchBatchDiagnosisKeysBasedOnInfectionLevel(warningType)

                val contextToken = BATCH + exposureNotificationRepository.processBatchDiagnosisKeys(archives)

                lastScheduledToken = contextToken

            } catch (e: Exception) {
                Timber.e(e, "Downloading new infection messages failed")
                downloadMessagesStateObserver.error(e)
            } finally {
                downloadMessagesStateObserver.idle()
            }
        }
    }

    override fun observeReceivedInfectionMessages(): Observable<List<DbReceivedInfectionMessage>> {
        return infectionMessageDao.observeReceivedInfectionMessages().asDbObservable()
    }

    override fun observeSentInfectionMessages(): Observable<List<DbSentInfectionMessage>> {
        return infectionMessageDao.observeSentInfectionMessages().asDbObservable()
    }

    override fun observeSomeoneHasRecoveredMessage(): Observable<Boolean> {
        return preferences.observeBoolean(PREF_SOMEONE_HAS_RECOVERED, false)
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