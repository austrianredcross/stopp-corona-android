package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import at.roteskreuz.stopcorona.constants.Constants.Behavior.EXPOSURE_MATCHING_FLEX_DURATION
import at.roteskreuz.stopcorona.constants.Constants.Behavior.EXPOSURE_MATCHING_INTERVAL
import at.roteskreuz.stopcorona.constants.Constants.Behavior.EXPOSURE_MATCHING_INTERVAL_END_TIME
import at.roteskreuz.stopcorona.constants.Constants.Behavior.EXPOSURE_MATCHING_START_TIME
import at.roteskreuz.stopcorona.constants.Constants.Behavior.EXPOSURE_MATCHING_TARGET_MINUTE
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.DiagnosisKeysRepository
import at.roteskreuz.stopcorona.utils.isInBetween
import at.roteskreuz.stopcorona.utils.millisTo
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
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

        private val firstAvailableMinute = (EXPOSURE_MATCHING_TARGET_MINUTE - EXPOSURE_MATCHING_FLEX_DURATION.toMinutes() / 2).toInt()
        private val lastAvailableMinute = (EXPOSURE_MATCHING_TARGET_MINUTE + EXPOSURE_MATCHING_FLEX_DURATION.toMinutes() / 2).toInt()
        private val firstAvailableTime = EXPOSURE_MATCHING_START_TIME
            .withMinute(EXPOSURE_MATCHING_TARGET_MINUTE)
            .minusMinutes(EXPOSURE_MATCHING_FLEX_DURATION.toMinutes() / 2)
        private val lastAvailableTime = EXPOSURE_MATCHING_INTERVAL_END_TIME
            .withMinute(EXPOSURE_MATCHING_TARGET_MINUTE)
            .plusMinutes(EXPOSURE_MATCHING_FLEX_DURATION.toMinutes() / 2)

        /**
         * Enqueue periodic work to run the exposure matching algorithm.
         * @param cancelCurrentAndScheduleNew True for Replace strategy with initial time in valid range.
         */
        fun enqueueExposurePeriodicMatching(workManager: WorkManager, cancelCurrentAndScheduleNew: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // internet access
                .build()
            val millisUntilNextRun = computeMillisUntilNextRun()
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
                if (cancelCurrentAndScheduleNew) {
                    ExistingPeriodicWorkPolicy.REPLACE
                } else {
                    ExistingPeriodicWorkPolicy.KEEP
                },
                request
            )
        }

        fun observeState(workManager: WorkManager): LiveData<MutableList<WorkInfo>> = workManager.getWorkInfosByTagLiveData(TAG)

        /**
         * Compute delay until the next possible hourly run in the 7:30 - 21:30 interval.
         * Actually the time to run is not precise because of flex time [EXPOSURE_MATCHING_FLEX_DURATION].
         */
        private fun computeMillisUntilNextRun(): Long {
            val now = LocalDateTime.now()

            // set correct minute
            var nextRunTime = when {
                // if between xx:00 - xx:15, then xx:15
                now.minute < firstAvailableMinute -> {
                    now.withMinute(firstAvailableMinute)
                }
                // if between xx:45 - xx:59, then (xx+1):15
                now.minute > lastAvailableMinute -> {
                    now.plusHours(1).withMinute(firstAvailableMinute)
                }
                // if between xx:15 - xx:45, then no change
                else -> {
                    now
                }
            }.truncatedTo(ChronoUnit.MINUTES) // ignore seconds, milliseconds, nanoseconds

            // set correct day
            nextRunTime = when {
                // if before 7:15, then at 7:15
                nextRunTime.toLocalTime().isAfter(lastAvailableTime) -> {
                    nextRunTime.with(firstAvailableTime)
                }
                // if after 21:45, then next day at 7:15
                nextRunTime.toLocalTime().isAfter(lastAvailableTime) -> {
                    nextRunTime.plusDays(1).with(firstAvailableTime)
                }
                // otherwise keep the time
                else -> {
                    nextRunTime
                }
            }
            Timber.d("Next Exposure matching run time is $nextRunTime")
            return now.millisTo(nextRunTime)
        }
    }

    private val workManager: WorkManager by inject()
    private val diagnosisKeysRepository: DiagnosisKeysRepository by inject()

    override suspend fun doWork(): Result {
        return if (LocalTime.now().isInBetween(firstAvailableTime, lastAvailableTime)) {
            try {
                diagnosisKeysRepository.fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()
            } catch (ex: Exception) {
                // we agreed to silently fail in case of errors here
                Timber.e(SilentError(ex))
            }

            Result.success()
        } else {
            // cancel and reschedule it for the next day morning
            // this will ensure to not run it during the night
            enqueueExposurePeriodicMatching(workManager, true)

            Result.failure()
        }
    }
}