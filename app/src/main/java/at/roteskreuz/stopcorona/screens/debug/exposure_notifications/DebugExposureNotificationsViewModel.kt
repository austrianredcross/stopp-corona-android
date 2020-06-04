package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiTemporaryTracingKey
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiVerificationPayload
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import io.reactivex.Observable
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

class DebugExposureNotificationsViewModel(
    appDispatchers : AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val contextInteractor: ContextInteractor
) : ScopedViewModel(appDispatchers)  {

    enum class DebugAction{
        REGISTER_WITH_FRAMEWORK{
            override fun requestCode(): Int { return  Constants.Request.EXPOSURE_NOTIFICATION_DEBUG_FRAGMENT + 1 }
        },
        REQUEST_EXPOSURE_KEYS{
            override fun requestCode(): Int { return  Constants.Request.EXPOSURE_NOTIFICATION_DEBUG_FRAGMENT + 2 }
        };

        abstract fun requestCode(): Int
    }



    private val exposureNotificationsEnabledSubject = NonNullableBehaviorSubject(false);
    private val exposureNotificationsTextSubject = NonNullableBehaviorSubject("no error");
    private val exposureNotificationsErrorState = DataStateObserver<Pair<Status,DebugAction>>()
    private val lastTemporaryExposureKeysSubject = NonNullableBehaviorSubject(mutableListOf<TemporaryExposureKey>());

    private val exposureNotificationClient: ExposureNotificationClient by lazy {
        Nearby.getExposureNotificationClient(contextInteractor.applicationContext);
    }

    fun checkEnabledState() {
        exposureNotificationClient.isEnabled()
            .addOnSuccessListener { enabled: Boolean ->
                exposureNotificationsEnabledSubject.onNext(enabled)
            }
            .addOnFailureListener { exception: Exception? ->
                Timber.e(
                    exception,
                    "could not get the current state of the exposure notifications SDK"
                )
                exposureNotificationsEnabledSubject.onNext(false)
                exposureNotificationsTextSubject.onNext("could not get the current state of the exposure notifications SDK: '${exception}'")
            }
    }

    fun observeEnabledState(): Observable<Boolean> {
        return exposureNotificationsEnabledSubject
    }

    fun observeResolutionError(): Observable<DataState<Pair<Status,DebugAction>>> {
        return exposureNotificationsErrorState.observe()
    }

    fun observeResultionErrorReasons(): Observable<String> {
        return exposureNotificationsTextSubject
    }

    fun observeLastTemporaryExposureKeys(): Observable<MutableList<TemporaryExposureKey>> {
        return lastTemporaryExposureKeysSubject
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
                exposureNotificationsTextSubject.onNext("")
            }
            .addOnFailureListener { exception: Exception? ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsEnabledSubject.onNext(false)
                    exposureNotificationsTextSubject.onNext("Unknown error when attempting to start API: '${exception}'")
                    return@addOnFailureListener
                }
                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result")
                    exposureNotificationsErrorState.loaded(Pair(exception.status, DebugAction.REGISTER_WITH_FRAMEWORK))
                    exposureNotificationsErrorState.idle()
                    exposureNotificationsTextSubject.onNext("Error, RESOLUTION_REQUIRED in result: '$exception'")
                    exposureNotificationsEnabledSubject.onNext(false)
                } else {
                    Timber.e(apiException, "No RESOLUTION_REQUIRED in result")
                    exposureNotificationsTextSubject.onNext("No RESOLUTION_REQUIRED in result: '$exception'")
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
                exposureNotificationsTextSubject.onNext("app unregistered from exposure notifications")
            }
            .addOnFailureListener { exception: java.lang.Exception? ->
                exposureNotificationsEnabledSubject.onNext(true)
                Timber.w(exception, "Failed to unregister")
                exposureNotificationsTextSubject.onNext("Failed to unregister from the Exposure Notifications framework: '$exception'")
            }

    }

    fun jumpToSystemSettings() {
        val intent = Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        contextInteractor.applicationContext.startActivity(intent)
    }

    fun googlePlayServicesVersion(): String {
        return contextInteractor.applicationContext.getString(
            R.string.debug_version_gms,
            getVersionNameForPackage(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)
        )
    }

    /** Gets the version name for a specified package. Returns a debug string if not found.  */
    private fun getVersionNameForPackage(packageName: String): String? {
        try {
            return contextInteractor.applicationContext.getPackageManager()
                .getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Couldn't get the app version")
        }
        return contextInteractor.applicationContext.getString(R.string.debug_version_not_available)
    }

    fun resolutionForRegistrationSucceeded(activity: Activity) {
        exposureNotificationsTextSubject.onNext("resolution succeded, trying to register again")
        startExposureNotifications(activity)
    }

    fun resolutionForExposureKeyHistorySucceded() {
        exposureNotificationsTextSubject.onNext("resolution succeded, getting the keys")
        getTemporaryExposureKeyHistory()
    }

    fun getTemporaryExposureKeyHistory() {
        exposureNotificationClient.temporaryExposureKeyHistory
            .addOnSuccessListener {
                Timber.d("got the list of Temporary Exposure Keys $it")
                exposureNotificationsTextSubject.onNext("got the list of Temp" +
                    "oraryExposureKeys $it")
                lastTemporaryExposureKeysSubject.onNext(it)

            }
            .addOnFailureListener { exception: Exception? ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsEnabledSubject.onNext(false)
                    exposureNotificationsTextSubject.onNext("Unknown error when attempting to start API: '${exception}'")
                    return@addOnFailureListener
                }
                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result")
                    exposureNotificationsErrorState.loaded(Pair(exception.status, DebugAction.REQUEST_EXPOSURE_KEYS))
                    exposureNotificationsErrorState.idle()
                    exposureNotificationsTextSubject.onNext("Error, RESOLUTION_REQUIRED in result: '$exception'")
                    exposureNotificationsEnabledSubject.onNext(false)
                } else {
                    Timber.e(apiException, "No RESOLUTION_REQUIRED in result")
                    exposureNotificationsTextSubject.onNext("No RESOLUTION_REQUIRED in result: '$exception'")
                    exposureNotificationsEnabledSubject.onNext(false)
                }
            }
    }

    fun resolutionForExposureKeyHistoryFailed(resultCode: Int) {
        exposureNotificationsTextSubject.onNext("resolutionForExposureKeyHistoryFailed with code: $resultCode")
    }

    fun resolutionForRegistrationFailed(resultCode: Int) {
        exposureNotificationsTextSubject.onNext("resolutionForRegistrationFailed with code: $resultCode")
    }

    fun uploadKeys(warningType: WarningType) {
        val keys = lastTemporaryExposureKeysSubject.value
        launch {
            val uploadData = emptyList<ApiTemporaryTracingKey>()
            try {
                apiInteractor.uploadInfectionData(
                    uploadData,
                    contextInteractor.packageName,
                    warningType,
                    ApiVerificationPayload(UUID.randomUUID().toString(),"RED-CROSS")
                )
                exposureNotificationsTextSubject.onNext("upload of ${keys.size}TEKs succeeded")
            } catch (e:Exception){
                exposureNotificationsTextSubject.onNext("upload of ${keys.size}TEKs failed because of $e")
                Timber.e(e)
            }
        }
    }
}