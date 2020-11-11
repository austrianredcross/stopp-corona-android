package at.roteskreuz.stopcorona.screens.dashboard

import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import at.roteskreuz.stopcorona.utils.startGooglePlayStore

fun DashboardFragment.exposureNotificationFrameSpecificErrorOnClick(exposureNotificationPhase : ExposureNotificationPhase) : Boolean {
    return when(exposureNotificationPhase) {
        is ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices -> {
            exposureNotificationPhase.googlePlayAvailability.getErrorDialog(
                requireActivity(),
                exposureNotificationPhase.googlePlayServicesStatusCode,
                DashboardFragment.REQUEST_CODE_FRAMEWORK_SERVICES_RESOLVE_ACTION
            ).show()
            true
        }
        is ExposureNotificationPhase.PrerequisitesError.InvalidVersionOfGooglePlayServices -> {
            startGooglePlayStore(Constants.ExposureNotification.GOOGLE_PLAY_SERVICES_PACKAGE_NAME)
            true
        }
        else -> false
    }
}