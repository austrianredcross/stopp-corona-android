package at.roteskreuz.stopcorona

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.hms.BuildConfig
import at.roteskreuz.stopcorona.hms.AbstractContactShieldIntentService
import at.roteskreuz.stopcorona.model.workers.ProcessDiagnosisKeysWorker
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class ContactShieldService : AbstractContactShieldIntentService(), KoinComponent {

    private val workManager: WorkManager by inject()

    override fun onExposureStateUpdated(context: Context, token: String) {

        if (BuildConfig.DEBUG) {
            showDebugNotificationProcessingFinished(context, token)
        }

        ProcessDiagnosisKeysWorker.enqueueProcessingOfDiagnosisKeys(workManager, token)
    }

    private fun showDebugNotificationProcessingFinished(context: Context, token: String) {
        val notification = NotificationCompat.Builder(context, Constants.NotificationChannels.CHANNEL_AUTOMATIC_DETECTION)
            .setContentTitle("processing done")
            .setSmallIcon(R.drawable.ic_red_cross)
            .setContentText("processing of $token finished")
            .setStyle(NotificationCompat.BigTextStyle().bigText("processing of $token finished"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(hashCode(), notification)
    }
}