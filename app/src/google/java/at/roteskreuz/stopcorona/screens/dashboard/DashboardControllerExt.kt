package at.roteskreuz.stopcorona.screens.dashboard

import android.content.Context
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import at.roteskreuz.stopcorona.screens.dashboard.epoxy.exposureNotificationError
import at.roteskreuz.stopcorona.utils.string
import com.airbnb.epoxy.EpoxyController

fun EpoxyController.buildFrameWorkSpecificPrerequisitesErrorCard(context : Context,
                                            phase: ExposureNotificationPhase,
                                            onExposureNotificationErrorActionClick: (ExposureNotificationPhase) -> Unit) : Boolean {

    return when(phase) {
        is ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices -> {
            exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
                id("unavailable_google_play_services")
                title(context.string(R.string.main_exposure_error_google_play_unavailable_title))
                fun addTryToResolveButtonIfPossible() {
                    if (phase.googlePlayAvailability.isUserResolvableError(phase.googlePlayServicesStatusCode)) {
                        action(context.string(R.string.main_exposure_error_google_play_unavailable_action))
                    }
                }
                when (phase) {
                    is ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceMissing -> {
                        description(context.string(R.string.main_exposure_error_google_play_unavailable_missing_message))
                        addTryToResolveButtonIfPossible()
                    }
                    is ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceUpdating -> {
                        description(context.string(R.string.main_exposure_error_google_play_unavailable_updating_message))
                        action(context.string(R.string.main_exposure_error_google_play_unavailable_updating_action))
                    }
                    is ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceVersionUpdateRequired -> {
                        description(context.string(R.string.main_exposure_error_google_play_unavailable_update_required_message))
                        action(context.string(R.string.main_exposure_error_google_play_unavailable_update_required_action))
                    }
                    is ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceDisabled -> {
                        description(context.string(R.string.main_exposure_error_google_play_unavailable_disabled_message))
                        addTryToResolveButtonIfPossible()
                    }
                    is ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceInvalid -> {
                        description(context.string(R.string.main_exposure_error_google_play_unavailable_invalid_message))
                        addTryToResolveButtonIfPossible()
                    }
                }
            }
            true
        }
        is ExposureNotificationPhase.PrerequisitesError.InvalidVersionOfGooglePlayServices -> {
            exposureNotificationError({ onExposureNotificationErrorActionClick(phase) }) {
                id("invalid_google_play_services_version")
                title(context.string(R.string.main_exposure_error_google_play_wrong_version_title))
                description(context.string(R.string.main_exposure_error_google_play_wrong_version_message))
                action(context.string(R.string.main_exposure_error_google_play_wrong_version_action_btn))
            }
            true
        }
        else -> false
    }
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

    when (phase) {
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.SignInRequired -> {
            exposureNotificationError(context.string(R.string.main_exposure_error_sign_in_message))
        }
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.InvalidAccount -> {
            exposureNotificationError(context.string(R.string.main_exposure_error_invalid_account_message))
        }
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.ResolutionRequired -> {
            // ignored, there is displayed a dialog
        }
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.ResolutionDeclined -> {
            exposureNotificationError(context.string(R.string.main_exposure_error_declined_message))
        }
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.NetworkError,
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.Interrupted,
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.Timeout,
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.Canceled -> {
            exposureNotificationError(context.string(R.string.main_exposure_error_network_error_message))
        }
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.InternalError,
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.Error,
        is ExposureNotificationPhase.FrameworkError.Critical.Unknown -> {
            exposureNotificationError(context.string(R.string.main_exposure_error_internal_message))
        }
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.DeveloperError,
        is ExposureNotificationPhase.FrameworkError.Critical.Gms.ApiNotConnected -> {
            exposureNotificationError(context.string(R.string.main_exposure_error_developer_message))
        }
        else -> return false
    }

    return true
}