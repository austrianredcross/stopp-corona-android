package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.repositories.InfectionMessengerRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Worker that displays local notifications.
 */
class DownloadInfectionMessagesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "DownloadInfectionMessagesWorker"

        /**
         * Enqueue download and processing infection messages.
         */
        fun enqueueDownloadInfection(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // internet access
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadInfectionMessagesWorker>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    private val infectionMessengerRepository: InfectionMessengerRepository by inject()

    override suspend fun doWork(): Result {

        infectionMessengerRepository.fetchDecryptAndStoreNewMessages()

        return Result.success()
    }
}