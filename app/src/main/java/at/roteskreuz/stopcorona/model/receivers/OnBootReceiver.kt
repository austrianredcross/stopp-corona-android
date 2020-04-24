package at.roteskreuz.stopcorona.model.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.model.services.CoronaDetectionService
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Start [CoronaDetectionService] when device is booted.
 */
class OnBootReceiver : BroadcastReceiver(), KoinComponent {

    private val notificationsRepository: NotificationsRepository by inject()

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        notificationsRepository.displayPleaseActivateAutomaticDetectionNotification()
    }
}