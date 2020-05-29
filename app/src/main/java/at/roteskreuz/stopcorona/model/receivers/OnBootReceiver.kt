package at.roteskreuz.stopcorona.model.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Display a notification to start exposure notification listeting when device is booted.
 */
//TODO: discuss onboot notification https://tasks.pxp-x.com/browse/CTAA-1548
class OnBootReceiver : BroadcastReceiver(), KoinComponent {

    private val notificationsRepository: NotificationsRepository by inject()

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        notificationsRepository.displayPleaseActivateAutomaticDetectionNotification()
    }
}