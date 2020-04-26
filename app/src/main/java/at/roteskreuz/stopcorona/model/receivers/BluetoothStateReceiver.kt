package at.roteskreuz.stopcorona.model.receivers

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.model.services.CoronaDetectionService
import at.roteskreuz.stopcorona.model.services.stopCoronaDetectionService
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Update [CoronaDetectionService] when bluetooth state changed.
 * Receiver listens only when [CoronaDetectionService] is running.
 */
class BluetoothStateReceiver : BroadcastReceiver(), Registrable, KoinComponent {

    companion object {
        private const val ACTION = BluetoothAdapter.ACTION_STATE_CHANGED
    }

    private val notificationsRepository: NotificationsRepository by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    // this won't happen if CoronaDetectionService is not running
                    notificationsRepository.displayPleaseActivateAutomaticDetectionNotification()
                }
                BluetoothAdapter.STATE_OFF -> {
                    context.stopCoronaDetectionService()
                    notificationsRepository.displayBluetoothIsOffAutomaticDetectionServiceCannotRunNotification()
                }
            }
        }
    }

    override fun register(context: Context) {
        context.registerReceiver(
            this,
            IntentFilter(ACTION)
        )
    }

    override fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }
}