package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.constants.Constants.ExposureNotification.EXPOSURE_ARCHIVES_FOLDER
import at.roteskreuz.stopcorona.model.entities.configuration.ApiConfiguration
import at.roteskreuz.stopcorona.model.entities.infection.exposure_keys.ApiIndexOfDiagnosisKeysArchives
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiInfectionDataRequest
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiTemporaryTracingKey
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiVerificationPayload
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.statistics.CovidStatistics
import at.roteskreuz.stopcorona.model.entities.tan.ApiRequestTan
import at.roteskreuz.stopcorona.model.entities.tan.ApiRequestTanBody
import at.roteskreuz.stopcorona.model.repositories.DataPrivacyRepository
import at.roteskreuz.stopcorona.model.repositories.FilesRepository
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.ExceptionMapperHelper
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.GeneralServerException
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.NoInternetConnectionException
import at.roteskreuz.stopcorona.skeleton.core.model.exceptions.UnexpectedError
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
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
    suspend fun getIndexOfDiagnosisKeysArchives(): ApiIndexOfDiagnosisKeysArchives

    /**
     * Save one file from the Content Delivery Network API to a local file.
     *
     * @return fileName
     */
    suspend fun downloadContentDeliveryFile(pathToArchive: String): String

    /**
     * Get the current AGES data from https://covid19-dashboard.ages.at/data/JsonData.json.
     */
    suspend fun downloadCovidStatistics(): CovidStatistics
}

class ApiInteractorImpl(
    private val appDispatchers: AppDispatchers,
    private val apiDescription: ApiDescription,
    private val tanApiDescription: TanApiDescription,
    private val contentDeliveryNetworkDescription: ContentDeliveryNetworkDescription,
    private val dataPrivacyRepository: DataPrivacyRepository,
    private val filesRepository: FilesRepository,
    private val agesApiDescription: AGESApiDescription
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

    override suspend fun getIndexOfDiagnosisKeysArchives(): ApiIndexOfDiagnosisKeysArchives {
        return withContext(appDispatchers.IO) {
            dataPrivacyRepository.assertDataPrivacyAccepted()
            checkGeneralErrors {
                contentDeliveryNetworkDescription.indexOfDiagnosisKeysArchives()
            }
        }
    }

    override suspend fun downloadContentDeliveryFile(pathToArchive: String): String {
        return withContext(appDispatchers.IO) {
            checkGeneralErrors {
                contentDeliveryNetworkDescription.downloadExposureKeyArchive(pathToArchive).use { body ->
                    val fileName = pathToArchive.replace("/", "-")
                    filesRepository.removeFile(EXPOSURE_ARCHIVES_FOLDER, fileName)
                    body.byteStream().use { inputStream ->
                        filesRepository.createFileFromInputStream(inputStream, EXPOSURE_ARCHIVES_FOLDER, fileName)
                    }
                    fileName
                }
            }
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

    override suspend fun downloadCovidStatistics(): CovidStatistics {
        return withContext(appDispatchers.IO){
            checkGeneralErrors {
                agesApiDescription.requestAGESData()
            }
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