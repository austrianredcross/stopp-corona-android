package at.roteskreuz.stopcorona.model

import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import timber.log.Timber

fun ExposureNotificationPhase.CheckingFrameworkError.checkFrameWorkSpecificError(exception: Throwable,
                                                                                 dependencyHolder: ExposureNotificationPhase.DependencyHolder,
                                                                                 moveToNextState: (ExposureNotificationPhase) -> Unit) : Boolean {
    if(exception !is ApiException)
        return false

    moveToNextState(
        when (exception.statusCode) {
            CommonStatusCodes.SIGN_IN_REQUIRED -> {
                Timber.e(SilentError("SIGN_IN_REQUIRED", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.SignInRequired(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.INVALID_ACCOUNT -> {
                Timber.e(SilentError("INVALID_ACCOUNT", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.InvalidAccount(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.RESOLUTION_REQUIRED -> {
                // no logging of error, this state is expect-able
                ExposureNotificationPhase.FrameworkError.Critical.Gms.ResolutionRequired(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.NETWORK_ERROR -> {
                Timber.e(SilentError("NETWORK_ERROR", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.NetworkError(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.INTERNAL_ERROR -> {
                Timber.e(SilentError("INTERNAL_ERROR", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.InternalError(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.DEVELOPER_ERROR -> {
                Timber.e(SilentError("DEVELOPER_ERROR", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.DeveloperError(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.ERROR -> {
                Timber.e(SilentError("ERROR", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.Error(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.INTERRUPTED -> {
                Timber.e(SilentError("INTERRUPTED", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.Interrupted(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.TIMEOUT -> {
                Timber.e(SilentError("TIMEOUT", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.Timeout(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.CANCELED -> {
                Timber.e(SilentError("CANCELED", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.Canceled(dependencyHolder,
                    exception, register)
            }
            CommonStatusCodes.API_NOT_CONNECTED -> {
                Timber.e(SilentError("API_NOT_CONNECTED", exception))
                ExposureNotificationPhase.FrameworkError.Critical.Gms.ApiNotConnected(dependencyHolder,
                    exception, register)
            }
            else -> {
                ExposureNotificationPhase.FrameworkError.Critical.Unknown(dependencyHolder,
                    exception, register)
            }
        }
    )
    return true
}