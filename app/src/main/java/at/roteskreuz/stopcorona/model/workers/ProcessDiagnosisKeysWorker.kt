package at.roteskreuz.stopcorona.model.workers

import android.content.Context
import androidx.work.*
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.DiagnosisKeysRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

/**
 * Worker that downloads and processes diagnosis keys.
 */
class ProcessDiagnosisKeysWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "ProcessDiagnosisKeysWorker"

        private const val ARGUMENT_TOKEN = "token"

        /**
         * Enqueue download and processing diagnosis keys.
         */
        fun enqueueProcessingOfDiagnosisKeys(workManager: WorkManager, token: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // internet access
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

    private val diagnosisKeysRepository: DiagnosisKeysRepository by inject()

    override suspend fun doWork(): Result {
        val token = inputData.getString(ARGUMENT_TOKEN)
        try {
            token?.let {
                diagnosisKeysRepository.processKeysBasedOnToken(token)
                return@doWork Result.success()
            }
            Timber.e(SilentError(IllegalArgumentException("No Token was provided, no work can be done")))
        } catch (ex: Exception){
            Timber.e(SilentError(ex))
        }
        return Result.failure()
    }
}