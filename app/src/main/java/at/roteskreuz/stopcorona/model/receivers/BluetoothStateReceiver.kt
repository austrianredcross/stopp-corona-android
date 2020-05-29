package at.roteskreuz.stopcorona.model.receivers

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Update [CoronaDetectionService] when bluetooth state changed.
 * Receiver listens only when [CoronaDetectionService] is running.
 */
// TODO: 28/05/2020 dusanjencik: Do we want to handle bluetooth state changes?
//   if so, let's implement it, otherwise let's remove this receiver.
//   https://tasks.pxp-x.com/browse/CTAA-1546 basucally covers this

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
                    // TODO: 28/05/2020 dusanjencik: Stop exposure notification?
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