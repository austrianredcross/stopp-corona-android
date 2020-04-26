package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.model.api.ApiError
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

/**
 * Repository for managing push messaging.
 */
interface PushMessagingRepository {

    /**
     * Connect to the push messaging cloud and start listening for changes.
     */
    suspend fun startListening()

    /**
     * Do an action when new push message is received.
     * @throws [ApiError.Critical.DataPrivacyNotAcceptedYet]
     */
    fun onMessageReceived()
}

class PushMessagingRepositoryImpl(
    private val firebaseMessaging: FirebaseMessaging,
    private val cryptoRepository: CryptoRepository,
    private val dataPrivacyRepository: DataPrivacyRepository,
    private val infectionMessengerRepository: InfectionMessengerRepository
) : PushMessagingRepository {

    override suspend fun startListening() {
        dataPrivacyRepository.assertDataPrivacyAccepted()
        Timber.d("Start connection to firebase")
        firebaseMessaging.subscribeToTopic(cryptoRepository.publicKeyPrefix)
            .addOnFailureListener {
                Timber.e(SilentError("Subscription to the firebase topic failed"))
            }
    }

    override fun onMessageReceived() {
        infectionMessengerRepository.enqueueDownloadingNewMessages()
    }
}
