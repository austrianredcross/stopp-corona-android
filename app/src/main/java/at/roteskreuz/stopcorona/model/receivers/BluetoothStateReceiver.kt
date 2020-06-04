package at.roteskreuz.stopcorona.model.receivers

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.model.workers.ExposureNotificationNotifierWorker
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Show notifications based on bluetooth state.
 */
class BluetoothStateReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        private const val ACTION = BluetoothAdapter.ACTION_STATE_CHANGED
    }

    private val workManager: WorkManager by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        ExposureNotificationNotifierWorker.enqueueAskForBluetoothIfDisabledAndFrameworkIsEnabled(workManager)
    }

    fun register(context: Context) {
        context.registerReceiver(
            this,
            IntentFilter(ACTION)
        )
    }

    fun unregisterFailSilent(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (e:Exception){
            // it only fails because it is already unregistered. So we can ignore any exception here
        }
    }
}