package at.roteskreuz.stopcorona.model

import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import com.huawei.hms.api.HuaweiApiAvailability

fun ExposureServiceStatus.toErrorPhaseOrNull(dependencyHolder: ExposureNotificationPhase.DependencyHolder): HuaweiErrorPhase? {
    if (this !is HuaweiServiceStatus) {
        throw IllegalArgumentException("Cannot convert status to phase: $this.")
    }

    val huaweiApiAvailability = HuaweiApiAvailability.getInstance()
    return when (this) {

        is HuaweiServiceStatus.Success -> null

        is HuaweiServiceStatus.DeviceTooOld -> {
            HuaweiErrorPhase.DeviceTooOld(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode,
                huaweiApiAvailability = huaweiApiAvailability
            )
        }
        is HuaweiServiceStatus.HmsCoreNotFound -> {
            HuaweiErrorPhase.HmsCoreNotFound(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode,
                huaweiApiAvailability = huaweiApiAvailability
            )
        }
        is HuaweiServiceStatus.OutOfDate -> {
            HuaweiErrorPhase.OutOfDate(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode,
                huaweiApiAvailability = huaweiApiAvailability
            )
        }
        is HuaweiServiceStatus.Unavailable -> {
            HuaweiErrorPhase.Unavailable(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode,
                huaweiApiAvailability = huaweiApiAvailability
            )
        }
        is HuaweiServiceStatus.UnofficialVersion -> {
            HuaweiErrorPhase.UnofficialVersion(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode,
                huaweiApiAvailability = huaweiApiAvailability
            )
        }
        is HuaweiServiceStatus.UnknownStatus -> {
            HuaweiErrorPhase.UnknownStatus(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode,
                huaweiApiAvailability = huaweiApiAvailability
            )
        }
    }

}
