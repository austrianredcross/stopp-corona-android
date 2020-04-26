package at.roteskreuz.stopcorona.model.services

import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.PushMessagingRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

/**
 * Firebase (push) messaging service.
 */
class StopCoronaFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val pushMessagingRepository: PushMessagingRepository by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        pushMessagingRepository.onMessageReceived()
    }

    override fun onNewToken(p0: String) {
        Timber.e(SilentError("onNewToken called"))
    }
}