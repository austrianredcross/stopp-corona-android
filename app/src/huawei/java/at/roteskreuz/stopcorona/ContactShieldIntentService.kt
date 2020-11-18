package at.roteskreuz.stopcorona

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.workers.ProcessDiagnosisKeysWorker
import com.huawei.hms.contactshield.ContactShieldCallback
import com.huawei.hms.contactshield.ContactShieldEngine
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class ContactShieldIntentService : IntentService("StoppCorona_ContactShieldIntentService"), KoinComponent {

    private val workManager: WorkManager by inject()
    private val contactShieldEngine: ContactShieldEngine by inject()
    private val callback: ContactShieldCallback = object : ContactShieldCallback {

        override fun onHasContact(token: String?) {
            val logText = "Has contact with '$token'."
            Timber.tag(LOG_TAG).d(logText)
            token?.let {
                onExposureStateUpdated(this@ContactShieldIntentService, logText)
            }
        }

        override fun onNoContact(token: String?) {
            val logText = "Has no contact with '$token'."
            Timber.tag(LOG_TAG).d(logText)
            token?.let {
                onExposureStateUpdated(this@ContactShieldIntentService, logText)
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            Timber.tag(LOG_TAG).w("Received intent 'null'.")
            return
        }

        contactShieldEngine.handleIntent(intent, callback)
    }

    private fun onExposureStateUpdated(context: Context, token: String) {
        if (BuildConfig.DEBUG) {
            showDebugNotificationProcessingFinished(context, token)
        }

        ProcessDiagnosisKeysWorker.enqueueProcessingOfDiagnosisKeys(workManager, token)
    }

    private fun showDebugNotificationProcessingFinished(context: Context, text: String) {
        val notification = NotificationCompat.Builder(context, Constants.NotificationChannels.CHANNEL_AUTOMATIC_DETECTION)
            .setContentTitle("Contact Shield processing done")
            .setSmallIcon(R.drawable.ic_red_cross)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(hashCode(), notification)
    }

    companion object {
        private const val LOG_TAG = "ContactShieldService"
    }
}