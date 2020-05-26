package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import at.roteskreuz.stopcorona.R
import com.google.android.apps.exposurenotification.nearby.ExposureNotificationClientWrapper
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import timber.log.Timber

class DebugExposureNotificationsViewModel(
    application: Application
): AndroidViewModel(application){
    private var exposureNotificationsEnabled: Boolean = false
    private var exposureNotificationsError: String = ""

    fun checkEnabledState(){
        ExposureNotificationClientWrapper.get(getApplication()).isEnabled()
            .addOnSuccessListener { enabled: Boolean ->
                exposureNotificationsEnabled = enabled
            }
            .addOnFailureListener { exception: Exception? ->
                //TODO: how do we handle this???
                exposureNotificationsEnabled = false;
            }
    }

    /**
     * Calls start on the Exposure Notifications API.
     */
    fun startExposureNotifications() {
        ExposureNotificationClientWrapper.get(getApplication())
            .start()
            .addOnSuccessListener { unused: Void? ->
                exposureNotificationsEnabled = true
            }
            .addOnFailureListener { exception: Exception? ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsError = "Unknown error when attempting to start API"
                    exposureNotificationsEnabled = false
                    return@addOnFailureListener
                }
                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result")
                    exposureNotificationsError = "Error, RESOLUTION_REQUIRED in result"
                    exposureNotificationsEnabled = false
                } else {
                    Timber.e(apiException,"No RESOLUTION_REQUIRED in result")
                    exposureNotificationsError = "No RESOLUTION_REQUIRED in result"
                    exposureNotificationsEnabled = false
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
                exposureNotificationsEnabled = false
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
}