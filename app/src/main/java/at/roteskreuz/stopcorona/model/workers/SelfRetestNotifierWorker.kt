package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.constants.Constants.Behavior.SELF_RETEST_END_TIME
import at.roteskreuz.stopcorona.constants.Constants.Behavior.SELF_RETEST_NOTIFICATION_INTERVAL
import at.roteskreuz.stopcorona.constants.Constants.Behavior.SELF_RETEST_START_TIME
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.utils.isInBetween
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Worker that reminds self testing to user by local notifications.
 */
class SelfRetestNotifierWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "SelfRetestNotifierWorker"

        /**
         * 8:15
         */
        private val firstAvailableTime = SELF_RETEST_START_TIME

        /**
         * 19:45
         */
        private val lastAvailableTime = SELF_RETEST_END_TIME


        /**
         * Enqueue displaying notification each 5 hours to do self testing.
         */
        fun enqueueSelfRetestingReminder(workManager: WorkManager) {
            val request =
                PeriodicWorkRequestBuilder<SelfRetestNotifierWorker>(SELF_RETEST_NOTIFICATION_INTERVAL.toMillis(), TimeUnit.MILLISECONDS)
                    .setInitialDelay(SELF_RETEST_NOTIFICATION_INTERVAL.toMillis(), TimeUnit.MILLISECONDS)
                    .addTag(TAG)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Cancel displaying notification to do self testing.
         */
        fun cancelSelfRetestingReminder(workManager: WorkManager) {
            workManager.cancelUniqueWork(TAG)
        }
    }

    private val notificationsRepository: NotificationsRepository by inject()

    override suspend fun doWork(): Result {

        if (LocalTime.now().isInBetween(firstAvailableTime, lastAvailableTime)){
            notificationsRepository.displaySelfRetestNotification()
        }

        return Result.success()
    }
}