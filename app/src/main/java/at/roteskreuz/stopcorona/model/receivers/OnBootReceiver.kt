package at.roteskreuz.stopcorona.model.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.model.workers.ExposureNotificationNotifierWorker
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Display a notification to turn on Bluetooth when the app is registered as the Exposure
 * Notifications application.
 */
class OnBootReceiver : BroadcastReceiver(), KoinComponent {

    private val workManager: WorkManager by inject()

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent?) {
        ExposureNotificationNotifierWorker.enqueueAskForBluetoothIfDisabledAndFrameworkIsEnabled(workManager)
    }
}