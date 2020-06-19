package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import android.content.Intent
import androidx.work.*
import at.roteskreuz.stopcorona.model.receivers.ExposureNotificationBroadcastReceiver
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.Duration
import java.util.concurrent.TimeUnit

/**
 * Worker that is scheduled for 5 minutes to call [ExposureNotificationBroadcastReceiver.onReceive] with
 * the token.
 * This is a hotfix which can be later deleted.
 * Currently Exposure framework can ignore calling of [ExposureNotificationBroadcastReceiver.onReceive]
 * if there is zero risk.
 */
class DelayedExposureBroadcastReceiverCallWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "DelayedExposureBroadcastReceiverCallWorker"
        private const val ARGUMENT_TOKEN = "token"

        /**
         * This worker is delayed for minimum 5 minutes, but workManager can decide to run it later.
         */
        private val DELAY = Duration.ofMinutes(5)

        /**
         * Enqueue download and processing diagnosis keys.
         */
        fun enqueueDelayedExposureReceiverCall(workManager: WorkManager, token: String) {
            val constraints = Constraints.Builder()
                .build()

            val request = OneTimeWorkRequestBuilder<DelayedExposureBroadcastReceiverCallWorker>()
                .setConstraints(constraints)
                .setInitialDelay(DELAY.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putString(ARGUMENT_TOKEN, token)
                        .build()
                )
                .addTag(TAG)
                .build()

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.APPEND,
                request
            )
        }
    }

    private val receiver: ExposureNotificationBroadcastReceiver by inject()

    override suspend fun doWork(): Result {
        val token = inputData.getString(ARGUMENT_TOKEN)
        // fake call to be sure that the zero risk is processed
        receiver.onReceive(
            applicationContext,
            Intent(ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED).apply {
                if (token != null) {
                    putExtra(ExposureNotificationClient.EXTRA_TOKEN, token)
                }
            }
        )

        return Result.failure()
    }
}