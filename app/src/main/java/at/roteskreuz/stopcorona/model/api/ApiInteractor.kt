package at.roteskreuz.stopcorona.model.api

import at.roteskreuz.stopcorona.model.entities.configuration.ApiConfiguration
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiInfectionInfoRequest
import at.roteskreuz.stopcorona.model.entities.infection.message.ApiInfectionMessages
import at.roteskreuz.stopcorona.model.entities.tan.ApiRequestTan
import at.roteskreuz.stopcorona.model.repositories.DataPrivacyRepository
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
     * Upload infection information about user.
     * @throws [SicknessCertificateUploadException], [ApiError]
     */
    suspend fun setInfectionInfo(infectionInfoRequest: ApiInfectionInfoRequest)

    /**
     * Request the server to send a TAN via text message
     * @throws [ApiError]
     *
     * @param mobileNumber The phonenumber to send the text message to
     * @return
     */
    suspend fun requestTan(mobileNumber: String): ApiRequestTan
}

class ApiInteractorImpl(
    private val appDispatchers: AppDispatchers,
    private val apiDescription: ApiDescription,
    private val dataPrivacyRepository: DataPrivacyRepository
) : ApiInteractor,
    ExceptionMapperHelper {

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

    override suspend fun setInfectionInfo(infectionInfoRequest: ApiInfectionInfoRequest) {
        withContext(appDispatchers.IO) {
            dataPrivacyRepository.assertDataPrivacyAccepted()
            checkGeneralErrors(
                { httpException ->
                    when (httpException.code()) {
                        HTTP_FORBIDDEN -> SicknessCertificateUploadException.TanInvalidException
                        HTTP_INTERNAL_ERROR -> SicknessCertificateUploadException.BirthdayInvalidException
                        else -> generalExceptionMapper(httpException)
                    }
                },
                {
                    apiDescription.infectionInfo(infectionInfoRequest)
                }
            )
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
                    apiDescription.requestTan(mobileNumber)
                })
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