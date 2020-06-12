package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.configuration.ApiConfiguration
import at.roteskreuz.stopcorona.model.entities.infection.exposure_keys.IndexOfDiagnosisKeysArchives
import at.roteskreuz.stopcorona.model.entities.infection.info.*
import at.roteskreuz.stopcorona.model.entities.infection.message.ApiInfectionMessages
import at.roteskreuz.stopcorona.model.entities.tan.ApiRequestTan
import at.roteskreuz.stopcorona.model.entities.tan.ApiRequestTanBody
import at.roteskreuz.stopcorona.model.repositories.DataPrivacyRepository
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.ExceptionMapperHelper
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.GeneralServerException
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.NoInternetConnectionException
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.UnexpectedError
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection.*

/**
 * Interactor that communicates with API.
 */
interface ApiInteractor {

    /**
     * Get the current configuration.
     * @throws [ApiError]
     */
    suspend fun getConfiguration(): ApiConfiguration

    /**
     * Get infection messages.
     * Returns the last if [fromId] is null.
     * Otherwise it returns one page of 100 messages from [fromId] (not included).
     *
     * Messages are filtered by our supplied [addressPrefix]
     *
     * @throws [ApiError]
     */
    suspend fun getInfectionMessages(addressPrefix: String, fromId: Long? = null): ApiInfectionMessages

    /**
     * Request the server to send a TAN via text message
     * @throws [ApiError]
     *
     * @param mobileNumber The phonenumber to send the text message to
     * @return
     */
    suspend fun requestTan(mobileNumber: String): ApiRequestTan

    /**
     * Upload infection data about the user.
     */
    suspend fun uploadInfectionData(
        temporaryTracingKeyList: List<ApiTemporaryTracingKey>,
        packageName: String,
        diagnosisType: WarningType,
        verificationPayload: ApiVerificationPayload
    )

    /**
     * retrieve listing of exposure key archives
     */
    suspend fun getIndexOfDiagnosisKeysArchives(): IndexOfDiagnosisKeysArchives

    suspend fun downloadContentDeliveryFileToTempFile(pathToArchive: String): File

    suspend fun fetchBatchDiagnosisKeysBasedOnInfectionLevel(warningType: WarningType): List<File>

    suspend fun fetchDailyBatchDiagnosisKeys(): List<List<File>>
}

class ApiInteractorImpl(
    private val appDispatchers: AppDispatchers,
    private val apiDescription: ApiDescription,
    private val contextInteractor: ContextInteractor,
    private val tanApiDescription: TanApiDescription,
    private val contentDeliveryNetworkDescription: ContentDeliveryNetworkDescription,
    private val dataPrivacyRepository: DataPrivacyRepository
) : ApiInteractor,
    ExceptionMapperHelper {

    companion object {
        private const val ANDROID_OS = "android"
        private const val VERIFICATION_AUTHORITY_NAME = "RedCross"
        private val regions = arrayListOf("AT")
    }

    private val generalExceptionMapper: (HttpException) -> Exception? = {
        when (it.code()) {
            HTTP_FORBIDDEN -> ApiError.Critical.AuthorizationError
            HTTP_GONE -> ApiError.Critical.ForceUpdate
            else -> null
        }
    }

    /**
     * Map http errors to application domain.
     * @throws [ApiError]
     */
    private suspend fun <T> checkGeneralErrors(
        httpExceptionMapper: (HttpException) -> Exception? = generalExceptionMapper,
        criticalAction: suspend () -> T
    ): T {
        return try {
            criticalAction()
        } catch (e: Exception) {
            throw resolveException(e, httpExceptionMapper)
        }
    }

    override suspend fun getConfiguration(): ApiConfiguration {
        return withContext(appDispatchers.IO) {
            dataPrivacyRepository.assertDataPrivacyAccepted()
            checkGeneralErrors {
                apiDescription.configuration().configuration
            }
        }
    }

    override suspend fun getInfectionMessages(addressPrefix: String, fromId: Long?): ApiInfectionMessages {
        return withContext(appDispatchers.IO) {
            dataPrivacyRepository.assertDataPrivacyAccepted()
            checkGeneralErrors {
                apiDescription.infectionMessages(addressPrefix, fromId)
            }
        }
    }

    override suspend fun getIndexOfDiagnosisKeysArchives(): IndexOfDiagnosisKeysArchives {
        return withContext(appDispatchers.IO) {
            dataPrivacyRepository.assertDataPrivacyAccepted()
            checkGeneralErrors {
                contentDeliveryNetworkDescription.indexOfDiagnosisKeysArchives()
            }
        }
    }

    override suspend fun downloadContentDeliveryFileToTempFile(pathToArchive: String): File {
        val response = contentDeliveryNetworkDescription.downloadExposureKeyArchive(pathToArchive).execute()
        if (response.isSuccessful) {
            val cacheDir = contextInteractor.applicationContext.getCacheDir()
            val outputFile: File = File(cacheDir, pathToArchive.replace("/", "-"))

            outputFile.deleteOnExit()

            response.body()?.byteStream()?.saveToFile(outputFile)
            return outputFile
        } else {
            response.message()
            throw IOException("it did not work code:${response} ")
        }
    }

    override suspend fun fetchBatchDiagnosisKeysBasedOnInfectionLevel(warningType: WarningType): List<File> {
        val indexOfArchives = getIndexOfDiagnosisKeysArchives()

        when (warningType) {
            WarningType.YELLOW, WarningType.RED -> {
                return indexOfArchives.full14DaysBatch.batchFilePaths.map {
                    Timber.d("Downloading the full 14 days batch, ther could revocations" +
                        "in the last 14 days even")
                    downloadContentDeliveryFileToTempFile(it)
                }
            }
            WarningType.REVOKE -> {
                return indexOfArchives.full07DaysBatch.batchFilePaths.map {
                    Timber.d("Downloading the full 7 days batch because there can´t" +
                        "be changes longer than 7 days ago as we were not exposed.")
                    downloadContentDeliveryFileToTempFile(it)
                }
            }
        }
    }

    override suspend fun fetchDailyBatchDiagnosisKeys(): List<List<File>> {
        val indexOfArchives = getIndexOfDiagnosisKeysArchives()

        //we assume the list of dailyBatches is sorted on the server!!!
        val listOfDaysWithDownloadedFilesSortedByServer = indexOfArchives.dailyBatches
            .map { dayBatch ->
                val downloadedFilesOfThisDay = dayBatch.batchFilePaths.map { filepathForOneDay ->
                    downloadContentDeliveryFileToTempFile(filepathForOneDay)
                }
                downloadedFilesOfThisDay
            }
        return listOfDaysWithDownloadedFilesSortedByServer
    }

    private fun InputStream.saveToFile(file: File) = use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    override suspend fun requestTan(mobileNumber: String): ApiRequestTan {
        return withContext(appDispatchers.IO) {
            dataPrivacyRepository.assertDataPrivacyAccepted()
            checkGeneralErrors({ httpException ->
                when (httpException.code()) {
                    HTTP_UNAUTHORIZED -> SicknessCertificateUploadException.PhoneNumberInvalidException
                    HTTP_INTERNAL_ERROR -> SicknessCertificateUploadException.SMSGatewayException
                    else -> generalExceptionMapper(httpException)
                }
            },
                {
                    tanApiDescription.requestTan(ApiRequestTanBody(mobileNumber))
                })
        }
    }

    override suspend fun uploadInfectionData(
        temporaryTracingKeyList: List<ApiTemporaryTracingKey>,
        packageName: String,
        diagnosisType: WarningType,
        verificationPayload: ApiVerificationPayload
    ) {
        withContext(appDispatchers.IO) {
            dataPrivacyRepository.assertDataPrivacyAccepted()
            checkGeneralErrors(
                { httpException ->
                    when (httpException.code()) {
                        HTTP_FORBIDDEN -> SicknessCertificateUploadException.TanInvalidException
                        else -> generalExceptionMapper(httpException)
                    }
                },
                {
                    apiDescription.publish(
                        ApiInfectionDataRequest(
                            temporaryTracingKeyList,
                            regions,
                            packageName,
                            ANDROID_OS,
                            diagnosisType,
                            VERIFICATION_AUTHORITY_NAME,
                            verificationPayload
                        )
                    )
                }
            )
        }
    }
}

/**
 * Exception in application domain.
 *
 * Can be also [GeneralServerException], [NoInternetConnectionException], [UnexpectedError].
 */
sealed class ApiError : Exception() {

    sealed class Critical : ApiError() {
        /**
         * Correct authorization required in http header.
         */
        object AuthorizationError : Critical()

        /**
         * API version discontinued. Client must be updated.
         */
        object ForceUpdate : Critical()

        /**
         * API call when data privacy is not accepted yet, which is violation of GDPR.
         */
        object DataPrivacyNotAcceptedYet : Critical()
    }
}

/**
 * Exceptions triggered when uploading infection info.
 */
sealed class SicknessCertificateUploadException : Exception() {

    /**
     * Triggered when the TAN used to upload the infection is invalid.
     */
    object TanInvalidException : SicknessCertificateUploadException()

    /**
     * Triggered when the birthday does not have the expected format (dd.MM.YYYY).
     */
    object BirthdayInvalidException : SicknessCertificateUploadException()

    /**
     * Triggered in case the phone number used for requesting a TAN is invalid.
     */
    object PhoneNumberInvalidException : SicknessCertificateUploadException()

    /**
     * Triggered in case of an SMS Gateway error.
     */
    object SMSGatewayException : SicknessCertificateUploadException()
}