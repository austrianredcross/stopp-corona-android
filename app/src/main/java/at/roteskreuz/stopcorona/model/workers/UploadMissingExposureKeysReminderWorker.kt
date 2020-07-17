package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.UploadMissingExposureKeys
import at.roteskreuz.stopcorona.utils.millisTo
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker to remind the user to upload his/hers keys from the day before.
 */
class UploadMissingExposureKeysReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "UploadKeysFromDayBeforeWorker"

        fun enqueueUploadMissingExposureKeysWorker(workManager: WorkManager, reminderTime: ZonedDateTime) {
            val now = ZonedDateTime.now()
            val millisBeforeReminder = now.millisTo(reminderTime)
            val request = OneTimeWorkRequestBuilder<UploadMissingExposureKeysReminderWorker>()
                .setInitialDelay(millisBeforeReminder, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelUploadMissingExposureKeysWorker(workManager: WorkManager) {
            workManager.cancelUniqueWork(TAG)
        }
    }

    private val notificationsRepository: NotificationsRepository by inject()
    private val quarantineRepository: QuarantineRepository by inject()

    override suspend fun doWork(): Result {
        val uploadMissingExposureKeys: UploadMissingExposureKeys? = quarantineRepository.observeIfUploadOfMissingExposureKeysIsNeeded()
            .blockingFirst().orElse(null)
        if (uploadMissingExposureKeys != null) {
            notificationsRepository.displayNotificationForUploadingMissingExposureKeyse(
                messageType = uploadMissingExposureKeys.messageType,
                dateWithMissingExposureKeys = uploadMissingExposureKeys.missingExposureKeyDate
            )
            return Result.success()
        }
        Timber.e(SilentError("uploadMissingExposureKeys is null"))
        return Result.failure()
    }
}
