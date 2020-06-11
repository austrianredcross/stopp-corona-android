package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.InfectionMessengerRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import kotlin.IllegalArgumentException

/**
 * Worker that displays local notifications.
 */
class ProcessDiagnosisKeysWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "DownloadInfectionMessagesWorker"

        private const val ARGUMENT_TOKEN = "token"

        /**
         * Enqueue download and processing infection messages.
         */
        fun enqueueDownloadInfection(workManager: WorkManager, token: String) {
            val constraints = Constraints.Builder()
                .build()

            val request = OneTimeWorkRequestBuilder<ProcessDiagnosisKeysWorker>()
                .setConstraints(constraints)
                .setInputData(
                    Data.Builder()
                        .putString(ARGUMENT_TOKEN, token)
                        .build()
                )
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
        val token = inputData.getString(ARGUMENT_TOKEN)

        token?.let {
            infectionMessengerRepository.processKeysbasedonToken(token)
            return@doWork Result.success()
        }
        Timber.e(SilentError(IllegalArgumentException("No Token was provided, no work can be done")))
        return Result.failure()
    }
}