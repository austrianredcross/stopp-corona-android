package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.apps.exposurenotification.nearby.ExposureNotificationClientWrapper
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import io.reactivex.Observable
import timber.log.Timber

class DebugExposureNotificationsViewModel(
    application: Application
    ): AndroidViewModel(application){

    private var exposureNotificationsError: String = ""
    private val exposureNotificationsEnabledSubject = NonNullableBehaviorSubject<Boolean>(false);
    private val exposureNotificationsErrorState = DataStateObserver<Status>()


    //TODO: move to a ExposureNotificationsRepository
    //TODO: get inspired by https://github.com/austrianredcross/stopp-corona-android/blob/develop/app/src/main/java/at/roteskreuz/stopcorona/model/repositories/DiscoveryRepository.kt
    fun checkEnabledState(){
        ExposureNotificationClientWrapper.get(getApplication()).isEnabled()
            .addOnSuccessListener { enabled: Boolean ->
                exposureNotificationsEnabledSubject.onNext(enabled)
            }
            .addOnFailureListener { exception: Exception? ->
                Timber.e(exception, "could not get the current state of the exposure notifications SDK")
                //TODO: how do we handle this???
                exposureNotificationsEnabledSubject.onNext(false)
            }
    }

    fun observeEnabledState(): Observable<Boolean> {
        return exposureNotificationsEnabledSubject
    }

    fun observeResolutionError(): Observable<DataState<Status>>{
        return exposureNotificationsErrorState.observe()
    }

    /**
     * Calls start on the Exposure Notifications API.
     */
    fun startExposureNotifications(activity: Activity) {
        exposureNotificationsErrorState.loading()
        ExposureNotificationClientWrapper.get(getApplication())
            .start()
            .addOnSuccessListener { unused: Void? ->
                exposureNotificationsEnabledSubject.onNext(true)
                exposureNotificationsErrorState.idle()
            }
            .addOnFailureListener { exception: Exception? ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsError = "Unknown error when attempting to start API"
                    exposureNotificationsEnabledSubject.onNext(false)
                    return@addOnFailureListener
                }
                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result")
                    exposureNotificationsErrorState.loaded(apiException.getStatus())
                    exposureNotificationsErrorState.idle()
                    exposureNotificationsError = "Error, RESOLUTION_REQUIRED in result"
                    exposureNotificationsEnabledSubject.onNext(false)
                } else {
                    Timber.e(apiException,"No RESOLUTION_REQUIRED in result")
                    exposureNotificationsError = "No RESOLUTION_REQUIRED in result"
                    exposureNotificationsEnabledSubject.onNext(false)
                }
            }
    }

    /**
     * Calls stop on the Exposure Notifications API.
     */
    fun stopExposureNotifications() {
        ExposureNotificationClientWrapper.get(getApplication())
            .stop()
            .addOnSuccessListener { unused: Void? ->
                exposureNotificationsEnabledSubject.onNext(false)
            }
            .addOnFailureListener { exception: java.lang.Exception? ->
                Timber.w(exception, "Failed to stop")
                exposureNotificationsError = "Failed to stop"
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
            return getApplication<Application>().getPackageManager().getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Couldn't get the app version")
        }
        return getApplication<Application>().getString(R.string.debug_version_not_available)
    }

    fun resolutionSucceeded() {
        exposureNotificationsEnabledSubject.onNext(true)
    }
}