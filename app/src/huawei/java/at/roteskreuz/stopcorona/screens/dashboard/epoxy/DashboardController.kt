package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.content.Context
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.HMS
import at.roteskreuz.stopcorona.model.HuaweiErrorPhase
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyController

fun EpoxyController.buildFrameWorkSpecificPrerequisitesErrorCard(context : Context,
                                                                 phase: ExposureNotificationPhase,
                                                                 onExposureNotificationErrorActionClick: (ExposureNotificationPhase) -> Unit) : Boolean {
    if(phase !is HuaweiErrorPhase)
        return false

    exposureNotificationError(onClick = { onExposureNotificationErrorActionClick(phase) },
        modelInitializer = {

            id("huawei_mobile_services_error")
            title(context.string(R.string.main_exposure_error_hms_unavailable_title))
            fun addTryToResolveButtonIfPossible() {
                if (phase.huaweiApiAvailability.isUserResolvableError(phase.huaweiServicesStatusCode)) {
                    action(context.string(R.string.main_exposure_error_hms_unavailable_action))
                }
            }

            val message = when (phase) {
                is HuaweiErrorPhase.DeviceTooOld -> context.string(
                    R.string.main_exposure_error_hms_device_too_old_message)
                is HuaweiErrorPhase.HmsCoreNotFound -> context.string(
                    R.string.main_exposure_error_hms_core_not_found_message)
                is HuaweiErrorPhase.OutOfDate -> context.string(
                    R.string.main_exposure_error_hms_out_of_date_message)
                is HuaweiErrorPhase.Unavailable -> context.string(
                    R.string.main_exposure_error_hms_unavailable_title)
                is HuaweiErrorPhase.UnofficialVersion -> context.string(
                    R.string.main_exposure_error_hms_unofficial_message)
                is HuaweiErrorPhase.UnknownStatus -> context.string(
                    R.string.main_exposure_error_hms_unknown_error_message)
            }

            description(message)
            addTryToResolveButtonIfPossible()
        })

    return true
}

fun EpoxyController.buildFrameWorkSpecificErrorCard(
    context: Context,
    phase: ExposureNotificationPhase,
    onExposureNotificationErrorActionClick: (ExposureNotificationPhase) -> Unit
) : Boolean {
    if(phase !is ExposureNotificationPhase.FrameworkError.Critical) {
        return false
    }

    fun exposureNotificationError(description: String) {
        exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
            id("exposure_notification_framework_error")
            title(context.string(R.string.main_exposure_error_title))
            description(description)
            action(context.string(R.string.main_exposure_error_action))
        }
    }

    return when(phase) {
        is HMS.ContactShieldDeclined -> {
            exposureNotificationError(context.string(R.string.main_exposure_error_declined_message))
            true
        }
        is HMS.LocationPermissionNotAllowedAllTheTime -> {
            exposureNotificationError(context.string(R.string.main_exposure_error_hms_location_permission_not_always_allowed))
            true
        }
        else -> false
    }
}