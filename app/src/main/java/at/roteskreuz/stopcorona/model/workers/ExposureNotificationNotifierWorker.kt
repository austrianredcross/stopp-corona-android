package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.repositories.BluetoothRepository
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Worker to show Bluetooth Notification based if the App is registered as the Exposure Notifications
 * application. Since it involves interaction with the Play Services, we need to run async.
 */
class ExposureNotificationNotifierWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "ExposureNotificationNotifierWorker"

        fun enqueueAskForBluetoothIfDisabledAndFrameworkIsEnabled(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<ExposureNotificationNotifierWorker>()
                .build()

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    private val notificationsRepository: NotificationsRepository by inject()
    private val exposureNotificationRepository: ExposureNotificationRepository by inject()
    private val bluetoothRepository: BluetoothRepository by inject()

    override suspend fun doWork(): Result {
        if (exposureNotificationRepository.refreshAndGetAppRegisteredForExposureNotificationsCurrentState() &&
            bluetoothRepository.bluetoothEnabled.not()) {
            notificationsRepository.displayPleaseActivateBluetoothNotification()
        }
        return Result.success()
    }
}