package at.roteskreuz.stopcorona.model

import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.common.ApiException
import com.huawei.hms.contactshield.StatusCode

sealed class HuaweiErrorPhase : ExposureNotificationPhase.PrerequisitesError() {

    abstract val huaweiServicesStatusCode: Int
    abstract val huaweiApiAvailability: HuaweiApiAvailability

    data class DeviceTooOld(
        override val dependencyHolder: DependencyHolder,
        override val huaweiServicesStatusCode: Int,
        override val huaweiApiAvailability: HuaweiApiAvailability
    ) : HuaweiErrorPhase()

    data class HmsCoreNotFound(
        override val dependencyHolder: DependencyHolder,
        override val huaweiServicesStatusCode: Int,
        override val huaweiApiAvailability: HuaweiApiAvailability
    ) : HuaweiErrorPhase()

    data class OutOfDate(
        override val dependencyHolder: DependencyHolder,
        override val huaweiServicesStatusCode: Int,
        override val huaweiApiAvailability: HuaweiApiAvailability
    ) : HuaweiErrorPhase()

    data class Unavailable(
        override val dependencyHolder: DependencyHolder,
        override val huaweiServicesStatusCode: Int,
        override val huaweiApiAvailability: HuaweiApiAvailability
    ) : HuaweiErrorPhase()

    data class UnofficialVersion(
        override val dependencyHolder: DependencyHolder,
        override val huaweiServicesStatusCode: Int,
        override val huaweiApiAvailability: HuaweiApiAvailability
    ) : HuaweiErrorPhase()

    data class UnknownStatus(
        override val dependencyHolder: DependencyHolder,
        override val huaweiServicesStatusCode: Int,
        override val huaweiApiAvailability: HuaweiApiAvailability
    ) : HuaweiErrorPhase()
}

sealed class HMS : ExposureNotificationPhase.FrameworkError.Critical() {
    data class ContactShieldDeclined(
        override val dependencyHolder: DependencyHolder,
        override val register: Boolean
    ) : FrameworkError.Critical()

    data class LocationPermissionNotAllowedAllTheTime(
            override val dependencyHolder: DependencyHolder,
            override val register: Boolean
    ) : FrameworkError.Critical()
}

fun ExposureNotificationPhase.CheckingFrameworkError.checkFrameWorkSpecificError(
    exception: Throwable,
    dependencyHolder: ExposureNotificationPhase.DependencyHolder,
    moveToNextState: (ExposureNotificationPhase) -> Unit
): Boolean {
    if (exception !is ApiException)
        return false

    moveToNextState(
        when (exception.statusCode) {
            StatusCode.STATUS_UNAUTHORIZED -> {
                HMS.ContactShieldDeclined(dependencyHolder, register)
            }
            StatusCode.STATUS_MISSING_PERMISSION_LOCATION -> {
                HMS.LocationPermissionNotAllowedAllTheTime(dependencyHolder, register)
            }
            else -> ExposureNotificationPhase.FrameworkError.Critical.Unknown(
                dependencyHolder,
                exception, register
            )
        }
    )

    return true
}