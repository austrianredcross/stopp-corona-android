package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import at.roteskreuz.stopcorona.constants.Constants.Behavior.EXPOSURE_MATCHING_FLEX_DURATION
import at.roteskreuz.stopcorona.constants.Constants.Behavior.EXPOSURE_MATCHING_INTERVAL
import at.roteskreuz.stopcorona.constants.Constants.Behavior.EXPOSURE_MATCHING_START_TIME
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.DiagnosisKeysRepository
import at.roteskreuz.stopcorona.utils.millisTo
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
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
         * 7:15
         */
        private val firstAvailableTime =
            EXPOSURE_MATCHING_START_TIME.minusMinutes(EXPOSURE_MATCHING_FLEX_DURATION.toMinutes() / 2)

        /**
         * Enqueue periodic work to run the exposure matching algorithm if no worker is enqueued yet.
         */
        fun enqueueExposurePeriodicMatching(
            workManager: WorkManager
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // internet access
                .build()
            val millisUntilNextRun = computeMillisUntilNextRun()
            val date = LocalDateTime.now() + Duration.ofMillis(millisUntilNextRun)
            Timber.d("Scheduling hourly exeposure matchings every hour from ${date}")
            val request = PeriodicWorkRequestBuilder<ExposureMatchingWorker>(
                repeatInterval = EXPOSURE_MATCHING_INTERVAL.toMillis(),
                repeatIntervalTimeUnit = TimeUnit.MILLISECONDS,
                flexTimeInterval = EXPOSURE_MATCHING_FLEX_DURATION.toMillis(),
                flexTimeIntervalUnit = TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .setInitialDelay(millisUntilNextRun, TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun observeState(workManager: WorkManager): LiveData<MutableList<WorkInfo>> =
            workManager.getWorkInfosByTagLiveData(TAG)

        /**
         * Compute delay until the next half xx:30
         */
        private fun computeMillisUntilNextRun(now: LocalDateTime = LocalDateTime.now()): Long {

            // set correct minute
            var firstRunTime = now.withMinute(30)
                .truncatedTo(ChronoUnit.MINUTES) // ignore seconds, milliseconds, nanoseconds

            // set correct hour if nextRunTime is in the past
            firstRunTime = if (firstRunTime > now) firstRunTime else firstRunTime.plusHours(1)

            val millisTillFirstRun = now.millisTo(firstRunTime).coerceAtLeast(0)
            Timber.d(
                "Time to first exposure matching: ${
                    TimeUnit.MILLISECONDS.toMinutes(
                        millisTillFirstRun
                    )
                }m"
            )
            return millisTillFirstRun
        }
    }

    override suspend fun doWork(): Result {
        Timber.d("Running exposure matching work")

        val diagnosisKeysRepository: DiagnosisKeysRepository by inject()

        if (LocalTime.now().isBefore(firstAvailableTime).not()) {
            try {
                diagnosisKeysRepository.fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()
            } catch (ex: Exception) {
                // we agreed to silently fail in case of errors here
                Timber.e(SilentError(ex))
            }

        } else {
            Timber.d("Skipping exposure matchings before $firstAvailableTime")
        }
        return Result.success()
    }
}