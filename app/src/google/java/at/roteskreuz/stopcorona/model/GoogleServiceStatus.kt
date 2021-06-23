package at.roteskreuz.stopcorona.model

import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus


sealed class GoogleServiceStatus : ExposureServiceStatus {

    data class Success(val statusCode: Int) : GoogleServiceStatus()
    object InvalidVersionOfGooglePlayServices : GoogleServiceStatus()
    data class ServiceMissing(val statusCode: Int) : GoogleServiceStatus()
    data class ServiceUpdating(val statusCode: Int) : GoogleServiceStatus()
    data class ServiceVersionUpdateRequired(val statusCode: Int) : GoogleServiceStatus()
    data class ServiceDisabled(val statusCode: Int) : GoogleServiceStatus()
    data class ServiceInvalid(val statusCode: Int) : GoogleServiceStatus()
    data class UnknownStatus(val statusCode: Int) : GoogleServiceStatus()

}