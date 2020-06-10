package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import org.koin.standalone.KoinComponent
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Worker that runs the exposure matching algorithm to detect if the user has been exposed
 * to a person infected or suspected to be infected with COVID-19.
 */
class ExposureMatchingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "ExposureMatchingWorker"

        /**
         * Enqueue periodic work to run the exposure matching algorithm.
         */
        fun enqueuePeriodExposureMatching(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // internet access
                .build()

            val request = PeriodicWorkRequestBuilder<ExposureMatchingWorker>(
                1, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            ).setConstraints(constraints)
                .addTag(TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    override suspend fun doWork(): Result {
        if (ZonedDateTime.now().isBefore(ZonedDateTime.now().withHour(7).withMinute(30))
            || ZonedDateTime.now().isAfter(ZonedDateTime.now().withHour(21).withMinute(30))
        ) {
            // The exposure matching is not run outside of the 7:30 - 21:30 interval.
            return Result.success()
        }

        // TODO mihbat 10-Jun: Run the exposure matching algorithm described in
        //  ticket https://tasks.pxp-x.com/browse/CTAA-1360 .

        return Result.success()
    }
}