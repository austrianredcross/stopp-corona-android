package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.utils.millisTo
import at.roteskreuz.stopcorona.utils.startOfTheDay
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Worker that reminds end of the quarantine to user by local notification.
 */
class EndQuarantineNotifierWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "EndQuarantineNotifierWorker"

        /**
         * Enqueue displaying notification once quarantine ended.
         */
        fun enqueueEndQuarantineReminder(workManager: WorkManager, endDateTime: ZonedDateTime) {
            // computing end of the quarantine
            // we set the end time as a midnight of the end date
            val now = ZonedDateTime.now()
            val endDay = endDateTime.startOfTheDay()
            val millisBeforeQuarantineEnd = now.millisTo(endDay)

            val request = OneTimeWorkRequestBuilder<EndQuarantineNotifierWorker>()
                .setInitialDelay(millisBeforeQuarantineEnd, TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build()

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /**
         * Cancel displaying notification to end of the quarantine.
         */
        fun cancelEndQuarantineReminder(workManager: WorkManager) {
            workManager.cancelUniqueWork(TAG)
        }
    }

    private val notificationsRepository: NotificationsRepository by inject()

    override suspend fun doWork(): Result {
        notificationsRepository.displayEndQuarantineNotification()

        return Result.success()
    }
}