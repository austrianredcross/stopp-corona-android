package at.roteskreuz.stopcorona.model.repositories

import android.content.SharedPreferences
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.db.dao.InfectionMessageDao
import at.roteskreuz.stopcorona.model.entities.infection.message.ApiInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.DbInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.InfectionMessageContent
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.workers.DownloadInfectionMessagesWorker
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.nullableLongSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeBoolean
import at.roteskreuz.stopcorona.utils.asDbObservable
import at.roteskreuz.stopcorona.utils.isInTheFuture
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
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
     * Start a process of downloading infection messages and trying to decrypt them.
     * Successful decrypted messages are stored in DB.
     */
    suspend fun fetchDecryptAndStoreNewMessages()

    /**
     * Observe the infection messages received.
     */
    fun observeReceivedInfectionMessages(): Observable<List<DbInfectionMessage>>

    /**
     * Observe the infection messages sent.
     */
    fun observeSentInfectionMessages(): Observable<List<DbInfectionMessage>>

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
}

class InfectionMessengerRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val infectionMessageDao: InfectionMessageDao,
    private val cryptoRepository: CryptoRepository,
    private val notificationsRepository: NotificationsRepository,
    private val preferences: SharedPreferences,
    private val quarantineRepository: QuarantineRepository,
    private val workManager: WorkManager
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
    private var lastMessageId: Long? by preferences.nullableLongSharedPreferencesProperty(PREF_LAST_MESSAGE_ID)

    private var someoneHasRecovered: Boolean by preferences.booleanSharedPreferencesProperty(PREF_SOMEONE_HAS_RECOVERED, false)

    override fun enqueueDownloadingNewMessages() {
        DownloadInfectionMessagesWorker.enqueueDownloadInfection(workManager)
    }

    override suspend fun storeSentInfectionMessages(messages: List<Pair<ByteArray, InfectionMessageContent>>) {
        infectionMessageDao.insertSentInfectionMessages(messages)
    }

    override suspend fun fetchDecryptAndStoreNewMessages() {
        if (downloadMessagesStateObserver.currentState is State.Loading) {
            return
        }

        downloadMessagesStateObserver.loading()
        withContext(coroutineContext) {
            try {
                val addressPrefix = cryptoRepository.publicKeyPrefix
                var messages: List<ApiInfectionMessage>
                do {
                    // downloading next 100 messages
                    messages = apiInteractor.getInfectionMessages(addressPrefix, lastMessageId).infectionMessages
                        ?: throw Exception("Message list is null")

                    // update pointer
                    val lastId = messages.maxBy { it.id }?.id
                    if (lastId == null) {
                        messages = listOf() // to stop loop
                        continue
                    }
                    lastMessageId = lastId

                    // try to decrypt them
                    messages.forEach { message ->
                        val decryptedMessage = cryptoRepository.decrypt(message.message)

                        // store decrypted messages to DB
                        if (decryptedMessage != null) {
                            val infectionMessageContent = InfectionMessageContent(decryptedMessage)
                            infectionMessageContent?.let {
                                if (infectionMessageContent.timeStamp.isInTheFuture().not()) {
                                    val dbMessage = it.asDbEntity().copy(isReceived = true)
                                    infectionMessageDao.insertOrUpdateInfectionMessage(dbMessage)
                                    when (val messageType = it.messageType) {
                                        is MessageType.InfectionLevel -> {
                                            notificationsRepository.displayInfectionNotification(messageType)
                                            quarantineRepository.receivedWarning(messageType.warningType)
                                        }
                                        MessageType.Revoke -> {
                                            setSomeoneHasRecovered()
                                            notificationsRepository.displaySomeoneHasRecoveredNotification()
                                        }
                                    }
                                }
                            }
                        }
                    }
                } while (messages.isNotEmpty()) // repeat until we have read all messages
            } catch (e: Exception) {
                Timber.e(e, "Downloading new infection messages failed")
                downloadMessagesStateObserver.error(e)
            } finally {
                downloadMessagesStateObserver.idle()
            }
        }
    }

    override fun observeReceivedInfectionMessages(): Observable<List<DbInfectionMessage>> {
        return infectionMessageDao.observeReceivedInfectionMessages().asDbObservable()
    }

    override fun observeSentInfectionMessages(): Observable<List<DbInfectionMessage>> {
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
}