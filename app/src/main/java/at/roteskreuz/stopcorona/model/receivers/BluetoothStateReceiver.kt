package at.roteskreuz.stopcorona.model.receivers

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.model.workers.EndQuarantineNotifierWorker
import at.roteskreuz.stopcorona.model.workers.ExposureNotificationNotifierWorker
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

/**
 * Show notifications based on bluetooth state
 */
class BluetoothStateReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        private const val ACTION = BluetoothAdapter.ACTION_STATE_CHANGED
    }

    private val workManager: WorkManager by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        ExposureNotificationNotifierWorker.enqueueExposureNotificationNotifierWorker(workManager)
    }

    fun register(context: Context) {
        context.registerReceiver(
            this,
            IntentFilter(ACTION)
        )
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }
}