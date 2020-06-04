package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import io.reactivex.Observable
import timber.log.Timber

class DebugExposureNotificationsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val exposureNotificationsEnabledSubject = NonNullableBehaviorSubject<Boolean>(false);
    private val exposureNotificationsErrorSubject = NonNullableBehaviorSubject<String>("no error");
    private val exposureNotificationsErrorState = DataStateObserver<Status>()

    private val exposureNotificationClient: ExposureNotificationClient by lazy {
        Nearby.getExposureNotificationClient(application);
    }

    fun checkEnabledState() {
        exposureNotificationClient.isEnabled()
            .addOnSuccessListener { enabled: Boolean ->
                exposureNotificationsEnabledSubject.onNext(enabled)
                exposureNotificationsErrorSubject.onNext("")
            }
            .addOnFailureListener { exception: Exception? ->
                Timber.e(
                    exception,
                    "could not get the current state of the exposure notifications SDK"
                )
                //TODO: how do we handle this???
                exposureNotificationsEnabledSubject.onNext(false)
                exposureNotificationsErrorSubject.onNext("could not get the current state of the exposure notifications SDK: '${exception}'")
            }
    }

    fun observeEnabledState(): Observable<Boolean> {
        return exposureNotificationsEnabledSubject
    }

    fun observeResolutionError(): Observable<DataState<Status>> {
        return exposureNotificationsErrorState.observe()
    }

    fun observeResultionErrorReasons(): Observable<String> {
        return exposureNotificationsErrorSubject
    }

    /**
     * Calls start on the Exposure Notifications API.
     */
    fun startExposureNotifications(activity: Activity) {
        exposureNotificationsErrorState.loading()
        exposureNotificationClient.start()
            .addOnSuccessListener { unused: Void? ->
                exposureNotificationsEnabledSubject.onNext(true)
                exposureNotificationsErrorState.idle()
                exposureNotificationsErrorSubject.onNext("")
            }
            .addOnFailureListener { exception: Exception? ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsEnabledSubject.onNext(false)
                    exposureNotificationsErrorSubject.onNext("Unknown error when attempting to start API: '${exception}'")
                    return@addOnFailureListener
                }
                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result")
                    exposureNotificationsErrorState.loaded(apiException.getStatus())
                    exposureNotificationsErrorState.idle()
                    exposureNotificationsErrorSubject.onNext("Error, RESOLUTION_REQUIRED in result: '$exception'")
                    exposureNotificationsEnabledSubject.onNext(false)
                } else {
                    Timber.e(apiException, "No RESOLUTION_REQUIRED in result")
                    exposureNotificationsErrorSubject.onNext("No RESOLUTION_REQUIRED in result: '$exception'")
                    exposureNotificationsEnabledSubject.onNext(false)
                }
            }
    }

    /**
     * Calls stop on the Exposure Notifications API.
     */
    fun stopExposureNotifications() {
        exposureNotificationClient.stop()
            .addOnSuccessListener { unused: Void? ->
                exposureNotificationsEnabledSubject.onNext(false)
            }
            .addOnFailureListener { exception: java.lang.Exception? ->
                exposureNotificationsEnabledSubject.onNext(true)
                Timber.w(exception, "Failed to stop")
                exposureNotificationsErrorSubject.onNext("Failed to stop the Exposure Notifications SDK: '$exception'")
            }

    }

    fun jumpToSystemSettings() {
        val intent = Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication<Application>().startActivity(intent)
    }

    fun googlePlayServicesVersion(): String {
        return getApplication<Application>().getString(
            R.string.debug_version_gms,
            getVersionNameForPackage(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)
        )
    }

    /** Gets the version name for a specified package. Returns a debug string if not found.  */
    private fun getVersionNameForPackage(packageName: String): String? {
        try {
            return getApplication<Application>().getPackageManager()
                .getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Couldn't get the app version")
        }
        return getApplication<Application>().getString(R.string.debug_version_not_available)
    }

    fun resolutionSucceeded(activity: Activity) {
        startExposureNotifications(activity)
    }

    fun getTemporaryExposureKeyHistory() {
        exposureNotificationClient.temporaryExposureKeyHistory
            .addOnSuccessListener {
                Timber.d("got the list of TemporaryExposureKeys $it")
            }
            .addOnFailureListener { exception: Exception? ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsEnabledSubject.onNext(false)
                    exposureNotificationsErrorSubject.onNext("Unknown error when attempting to start API: '${exception}'")
                    return@addOnFailureListener
                }
                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result")
                    exposureNotificationsErrorState.loaded(apiException.getStatus())
                    exposureNotificationsErrorState.idle()
                    exposureNotificationsErrorSubject.onNext("Error, RESOLUTION_REQUIRED in result: '$exception'")
                    exposureNotificationsEnabledSubject.onNext(false)
                } else {
                    Timber.e(apiException, "No RESOLUTION_REQUIRED in result")
                    exposureNotificationsErrorSubject.onNext("No RESOLUTION_REQUIRED in result: '$exception'")
                    exposureNotificationsEnabledSubject.onNext(false)
                }
            }
    }
}