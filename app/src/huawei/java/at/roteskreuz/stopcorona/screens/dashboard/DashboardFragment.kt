package at.roteskreuz.stopcorona.screens.dashboard

import at.roteskreuz.stopcorona.model.HuaweiErrorPhase
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase

fun DashboardFragment.exposureNotificationFrameSpecificErrorOnClick(exposureNotificationPhase: ExposureNotificationPhase) : Boolean {
    return when(exposureNotificationPhase) {
        is HuaweiErrorPhase -> {
            exposureNotificationPhase.huaweiApiAvailability.getErrorDialog(
                requireActivity(),
                exposureNotificationPhase.huaweiServicesStatusCode,
                DashboardFragment.REQUEST_CODE_FRAMEWORK_SERVICES_RESOLVE_ACTION
            ).show()
            true
        }
        else -> false
    }
}