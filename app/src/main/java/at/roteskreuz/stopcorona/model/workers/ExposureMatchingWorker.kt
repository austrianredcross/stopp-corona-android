package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.InfectionMessengerRepository
import at.roteskreuz.stopcorona.utils.minus
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.lang.Exception
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

            val request = OneTimeWorkRequestBuilder<ExposureMatchingWorker>()
                .setConstraints(constraints)
                .setInitialDelay(computeDelayUntilNextRun().seconds, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        /**
         * Compute delay until the next possible hourly run in the 7:30 - 21:30 interval.
         */
        private fun computeDelayUntilNextRun(): Duration {
            val nextPossibleRun = ZonedDateTime.now().plusHours(1).withMinute(30)
            val plannedRun = when {
                // If is after 21:30, schedule for next day at 7:30.
                nextPossibleRun.isAfter(ZonedDateTime.now().withHour(21).withMinute(30)) -> {
                    ZonedDateTime.now().plusDays(1).withHour(7).withMinute(30)
                }
                // If is before 7:30, schedule for current day at 7:30.
                nextPossibleRun.isBefore(ZonedDateTime.now().withHour(7).withMinute(30)) -> {
                    ZonedDateTime.now().withHour(7).withMinute(30)
                }
                // Otherwise the possible run is in 7:30 - 9:30 and can be scheduled as it is.
                else -> {
                    nextPossibleRun
                }
            }
            return plannedRun.minus(ZonedDateTime.now())
        }
    }

    private val workManager: WorkManager by inject()
    private val infectionMessengerRepository: InfectionMessengerRepository by inject()

    override suspend fun doWork(): Result {
        try {
            infectionMessengerRepository.fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()
        } catch (ex: Exception){
            Timber.e(SilentError(ex))
        }
        // Schedule the next exposure matching work.
        enqueueNextExposureMatching(workManager)
        return Result.success()
    }
}