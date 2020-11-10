package at.roteskreuz.stopcorona.model

import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase

fun ExposureServiceStatus.toErrorPhaseOrNull(dependencyHolder: ExposureNotificationPhase.DependencyHolder): ExposureNotificationPhase.PrerequisitesError.HuaweiErrorPhase? {

    if (this !is HuaweiServiceStatus) {
        throw IllegalArgumentException("Cannot convert status to phase: $this.")
    }
    return when (this) {

        is HuaweiServiceStatus.Success -> null

        is HuaweiServiceStatus.DeviceTooOld -> {
            ExposureNotificationPhase.PrerequisitesError.HuaweiErrorPhase.DeviceTooOld(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode
            )
        }
        is HuaweiServiceStatus.HmsCoreNotFound -> {
            ExposureNotificationPhase.PrerequisitesError.HuaweiErrorPhase.HmsCoreNotFound(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode
            )
        }
        is HuaweiServiceStatus.OutOfDate -> {
            ExposureNotificationPhase.PrerequisitesError.HuaweiErrorPhase.OutOfDate(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode
            )
        }
        is HuaweiServiceStatus.Unavailable -> {
            ExposureNotificationPhase.PrerequisitesError.HuaweiErrorPhase.Unavailable(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode
            )
        }
        is HuaweiServiceStatus.UnofficialVersion -> {
            ExposureNotificationPhase.PrerequisitesError.HuaweiErrorPhase.UnofficialVersion(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode
            )
        }
        is HuaweiServiceStatus.UnknownStatus -> {
            ExposureNotificationPhase.PrerequisitesError.HuaweiErrorPhase.UnknownStatus(
                dependencyHolder = dependencyHolder,
                huaweiServicesStatusCode = this.statusCode
            )
        }

    }

}
