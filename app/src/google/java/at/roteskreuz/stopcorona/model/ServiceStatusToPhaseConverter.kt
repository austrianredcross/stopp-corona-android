package at.roteskreuz.stopcorona.model

import at.roteskreuz.stopcorona.commonexposure.ExposureServiceStatus
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import at.roteskreuz.stoppcorona.google.GoogleServiceStatus
import com.google.android.gms.common.GoogleApiAvailability

fun ExposureServiceStatus.toErrorPhaseOrNull(dependencyHolder: ExposureNotificationPhase.DependencyHolder): ExposureNotificationPhase.PrerequisitesError? {

    if (this !is GoogleServiceStatus) {
        throw IllegalArgumentException("Cannot convert status to phase: $this.")
    }

    val googlePlayAvailability = GoogleApiAvailability.getInstance()

    return when (this) {

        is GoogleServiceStatus.Success -> null

        is GoogleServiceStatus.InvalidVersionOfGooglePlayServices -> {
            ExposureNotificationPhase.PrerequisitesError.InvalidVersionOfGooglePlayServices(
                dependencyHolder = dependencyHolder
            )
        }

        is GoogleServiceStatus.ServiceMissing -> {
            ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceMissing(
                dependencyHolder = dependencyHolder,
                googlePlayAvailability = googlePlayAvailability,
                googlePlayServicesStatusCode = this.statusCode
            )
        }
        is GoogleServiceStatus.ServiceUpdating -> {
            ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceUpdating(
                dependencyHolder = dependencyHolder,
                googlePlayAvailability = googlePlayAvailability,
                googlePlayServicesStatusCode = this.statusCode
            )
        }
        is GoogleServiceStatus.ServiceVersionUpdateRequired -> {
            ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceVersionUpdateRequired(
                dependencyHolder = dependencyHolder,
                googlePlayAvailability = googlePlayAvailability,
                googlePlayServicesStatusCode = this.statusCode
            )
        }
        is GoogleServiceStatus.ServiceDisabled -> {
            ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceDisabled(
                dependencyHolder = dependencyHolder,
                googlePlayAvailability = googlePlayAvailability,
                googlePlayServicesStatusCode = this.statusCode
            )
        }
        is GoogleServiceStatus.UnknownStatus -> {
            ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceInvalid(
                dependencyHolder = dependencyHolder,
                googlePlayAvailability = googlePlayAvailability,
                googlePlayServicesStatusCode = this.statusCode
            )
        }
        is GoogleServiceStatus.ServiceInvalid -> {
            ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.ServiceInvalid(
                dependencyHolder = dependencyHolder,
                googlePlayAvailability = googlePlayAvailability,
                googlePlayServicesStatusCode = this.statusCode
            )
        }
    }

}