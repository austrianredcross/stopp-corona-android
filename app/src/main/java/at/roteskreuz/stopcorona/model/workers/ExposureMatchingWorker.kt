package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.DiagnosisKeysRepository
import at.roteskreuz.stopcorona.utils.millisTo
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.ZonedDateTime
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
         * Enqueue periodic work to run the exposure matching algorithm.
         */
        fun enqueueNextExposureMatching(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // internet access
                .build()
            val millisUntilNextRun = computeMillisUntilNextRun()
            val request = OneTimeWorkRequestBuilder<ExposureMatchingWorker>()
                .setConstraints(constraints)
                .setInitialDelay(millisUntilNextRun, TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build()

            workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        /**
         * Compute delay until the next possible hourly run in the 7:30 - 21:30 interval.
         */
        private fun computeMillisUntilNextRun(): Long {
            val now = ZonedDateTime.now()
            // Forward to next hour if past the half hour
            val hourOfNextHalfHour = now.plusMinutes(30)
            // Go to half hour
            val nextHalfHour = hourOfNextHalfHour.withMinute(30).withSecond(0).withNano(0)
            val plannedRun = when {
                // If nextHalfHour is after 21:30, schedule for next day at 7:30.
                nextHalfHour.isAfter(nextHalfHour.withHour(21).withMinute(30)) -> {
                    nextHalfHour.plusDays(1).withHour(7).withMinute(30)
                }
                // If nextHalfHour is before 7:30, schedule for same day at 7:30.
                nextHalfHour.isBefore(nextHalfHour.withHour(7).withMinute(30)) -> {
                    nextHalfHour.withHour(7).withMinute(30)
                }
                // Otherwise the possible run is in 7:30 - 21:30 and can be scheduled as it is.
                else -> {
                    nextHalfHour
                }
            }
            return now.millisTo(plannedRun)
        }
    }

    private val workManager: WorkManager by inject()
    private val diagnosisKeysRepository: DiagnosisKeysRepository by inject()

    override suspend fun doWork(): Result {
        // Reschedule first thing in case we get killed later on
        enqueueNextExposureMatching(workManager)

        try {
            diagnosisKeysRepository.fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()
        } catch (ex: Exception) {
            //we agreed to silently fail in case of errors here
            Timber.e(SilentError(ex))
        }
        return Result.success()
    }
}