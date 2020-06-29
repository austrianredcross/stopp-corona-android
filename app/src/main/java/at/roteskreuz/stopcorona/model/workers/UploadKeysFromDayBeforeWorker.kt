package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.repositories.BluetoothRepository
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.utils.millisUntilTheStartOfTheNextUtcDay
import at.roteskreuz.stopcorona.utils.minus
import at.roteskreuz.stopcorona.utils.startOfTheDay
import at.roteskreuz.stopcorona.utils.startOfTheUtcDay
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Worker to remind the user to upload his/hers keys from the day before.
 */
class UploadKeysFromDayBeforeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "UploadKeysFromDayBeforeWorker"


        fun enqueueUploadKeysFromDayBeforeWorkerOnTheStartOfTheNextUtcDay(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<UploadKeysFromDayBeforeWorker>()
                .setInitialDelay(ZonedDateTime.now().millisUntilTheStartOfTheNextUtcDay(), TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    private val notificationsRepository: NotificationsRepository by inject()

    override suspend fun doWork(): Result {
        notificationsRepository.displayNotificationForUploadingKeysFromTheDayBefore()
        return Result.success()
    }
}
