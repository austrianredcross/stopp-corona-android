package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.constants.Constants.ExposureNotification.ACTION_EXPOSURE_STATE_UPDATED_BROADCAST_TIMEOUT
import at.roteskreuz.stopcorona.model.receivers.ExposureNotificationBroadcastReceiver
import org.koin.standalone.KoinComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker that is scheduled for [ACTION_EXPOSURE_STATE_UPDATED_BROADCAST_TIMEOUT] minutes to call
 * [ExposureNotificationBroadcastReceiver.onExposureStateUpdated] with the token.
 * This is a hotfix which can be later deleted.
 * Currently Exposure framework can drop broadcasts if there are zero matched keys risk.
 */
class DelayedExposureBroadcastReceiverCallWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "DelayedExposureBroadcastReceiverCallWorker"
        private const val ARGUMENT_TOKEN = "token"

        /**
         * Enqueue download and processing diagnosis keys.
         */
        fun enqueueDelayedExposureReceiverCall(workManager: WorkManager, token: String) {
            val constraints = Constraints.Builder()
                .build()

            val request = OneTimeWorkRequestBuilder<DelayedExposureBroadcastReceiverCallWorker>()
                .setConstraints(constraints)
                .setInitialDelay(ACTION_EXPOSURE_STATE_UPDATED_BROADCAST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putString(ARGUMENT_TOKEN, token)
                        .build()
                )
                .addTag(TAG)
                .build()

            workManager.enqueueUniqueWork(
                "$TAG-$token",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val token = inputData.getString(ARGUMENT_TOKEN)
        Timber.d("ENStatusUpdates: Timeout for $token expired, let's check if it has been processed")
        // fake call to be sure that the zero risk is processed
        token?.let {
            ExposureNotificationBroadcastReceiver.onExposureStateUpdated(applicationContext, token)
        }

        return Result.success()
    }
}