package at.roteskreuz.stopcorona.hms

import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus

sealed class HuaweiServiceStatus : ExposureServiceStatus {

    abstract val statusCode: Int

    data class Success(override val statusCode: Int) : HuaweiServiceStatus()
    data class HmsCoreNotFound(override val statusCode: Int) : HuaweiServiceStatus()
    data class OutOfDate(override val statusCode: Int) : HuaweiServiceStatus()
    data class Unavailable(override val statusCode: Int) : HuaweiServiceStatus()
    data class UnofficialVersion(override val statusCode: Int) : HuaweiServiceStatus()
    data class DeviceTooOld(override val statusCode: Int) : HuaweiServiceStatus()
    data class UnknownStatus(override val statusCode: Int) : HuaweiServiceStatus()

}