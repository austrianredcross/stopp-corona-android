package at.roteskreuz.stopcorona.screens.debug.contact_tracing

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.android.apps.exposurenotification.nearby.ExposureNotificationClientWrapper
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import timber.log.Timber

class DebugContactNotificationsViewModel(
    application: Application,
    private var exposureNotificationsEnabled: Boolean,
    private var exposureNotificationsError: String = ""
): AndroidViewModel(application){

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
}