package at.roteskreuz.stopcorona.screens.debug.exposure_notifications

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiTemporaryTracingKey
import at.roteskreuz.stopcorona.model.entities.infection.info.ApiVerificationPayload
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ResolutionType
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import io.reactivex.Observable
import kotlinx.coroutines.launch
import timber.log.Timber

class DebugExposureNotificationsViewModel(
    appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val contextInteractor: ContextInteractor,
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val exposureNotificationClient: CommonExposureClient
) : ScopedViewModel(appDispatchers) {

    private val exposureNotificationsEnabledSubject = NonNullableBehaviorSubject(false)
    private val exposureNotificationsTextSubject = NonNullableBehaviorSubject("no error")
    private val exposureNotificationsErrorState = DataStateObserver<ResolutionType>()
    private val lastTemporaryExposureKeysSubject = NonNullableBehaviorSubject<List<TemporaryExposureKey>>(emptyList())
    private val tanRequestUUIDSubject = NonNullableBehaviorSubject("no-tan")

    fun checkEnabledState() {

        launch {
            try {
                val isServiceRunning = exposureNotificationClient.isRunning()
                exposureNotificationsEnabledSubject.onNext(isServiceRunning)

            } catch (exception: Exception) {
                Timber.e(exception, "could not get the current state of the exposure notifications SDK")

                exposureNotificationsEnabledSubject.onNext(false)
                exposureNotificationsTextSubject.onNext("could not get the current state of the exposure notifications SDK: '${exception}'")

            }
        }

    }

    fun observeEnabledState(): Observable<Boolean> {
        return exposureNotificationsEnabledSubject
    }

    fun observeResolutionError(): Observable<DataState<ResolutionType>> {
        return exposureNotificationsErrorState.observe()
    }

    fun observeResultionErrorReasons(): Observable<String> {
        return exposureNotificationsTextSubject
    }

    fun observeLastTemporaryExposureKeys(): Observable<List<TemporaryExposureKey>> {
        return lastTemporaryExposureKeysSubject
    }

    /**
     * Calls start on the Exposure Notifications API.
     */
    fun startExposureNotifications(activity: Activity) {
        exposureNotificationsErrorState.loading()

        launch {

            try {
                exposureNotificationClient.start()

                exposureNotificationsEnabledSubject.onNext(true)
                exposureNotificationsErrorState.idle()
                exposureNotificationsTextSubject.onNext("")
            } catch (exception: Exception) {

                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsEnabledSubject.onNext(false)
                    exposureNotificationsTextSubject.onNext("Unknown error when attempting to start API: '${exception}'")
                    return@launch
                }

                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result but not handled in UI")
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
    }

    /**
     * Calls stop on the Exposure Notifications API.
     */
    fun stopExposureNotifications() {

        launch {
            try {
                exposureNotificationClient.stop()
                exposureNotificationsEnabledSubject.onNext(false)
                exposureNotificationsTextSubject.onNext("app unregistered from exposure notifications")
            } catch (exception: Exception) {
                exposureNotificationsEnabledSubject.onNext(true)
                Timber.w(exception, "Failed to unregister")
                exposureNotificationsTextSubject.onNext("Failed to unregister from the Exposure Notifications framework: '$exception'")

            }

        }

    }

    fun getServicesVersion(context: Context): String {
        return exposureNotificationClient.getServiceVersion(context)
    }

    fun jumpToSystemSettings() {
        val intent = exposureNotificationRepository.getExposureSettingsIntent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contextInteractor.applicationContext.startActivity(intent)
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

        launch {

            try {
                val keys = exposureNotificationClient.getTemporaryExposureKeys()
                Timber.d("got the list of Temporary Exposure Keys $keys")
                exposureNotificationsTextSubject.onNext("got the list of TemporaryExposureKeys $keys")
                lastTemporaryExposureKeysSubject.onNext(keys)
            } catch (exception: Exception) {
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsEnabledSubject.onNext(false)
                    exposureNotificationsTextSubject.onNext("Unknown error when attempting to start API: '${exception}'")
                    return@launch
                }
                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result")
                    exposureNotificationsErrorState.loaded(ResolutionType.GetExposureKeys(apiException.status))
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


    }

    fun resolutionForExposureKeyHistoryFailed(resultCode: Int) {
        exposureNotificationsTextSubject.onNext("resolutionForExposureKeyHistoryFailed with code: $resultCode")
    }

    fun resolutionForRegistrationFailed(resultCode: Int) {
        exposureNotificationsTextSubject.onNext("resolutionForRegistrationFailed with code: $resultCode")
    }

    fun requestTan(mobileNumber: String) {
        launch {
            try {
                val tanRequestUUID = apiInteractor.requestTan(mobileNumber).uuid
                tanRequestUUIDSubject.onNext(tanRequestUUID)
                exposureNotificationsTextSubject.onNext("TAN for $mobileNumber was requested with UUID $tanRequestUUID")
            } catch (e: Exception) {
                exposureNotificationsTextSubject.onNext("TAN for  $mobileNumber failed because of $e")
                Timber.e(e)
            }
        }
    }

    fun uploadKeys(warningType: WarningType, tan: String) {
        val keys = lastTemporaryExposureKeysSubject.value
        launch {
            try {
                val keysWithoutPasswords = lastTemporaryExposureKeysSubject.value.map {
                    val base64key = Base64.encodeToString(it.keyData, Base64.NO_WRAP)
                    ApiTemporaryTracingKey(
                        key = base64key,
                        password = base64key,
                        intervalNumber = it.rollingStartIntervalNumber,
                        intervalCount = it.rollingPeriod
                    )
                }
                apiInteractor.uploadInfectionData(
                    keysWithoutPasswords,
                    contextInteractor.packageName,
                    warningType,
                    ApiVerificationPayload(tanRequestUUIDSubject.value, tan)
                )
                exposureNotificationsTextSubject.onNext("upload of ${keys.size}TEKs succeeded")
            } catch (e: Exception) {
                exposureNotificationsTextSubject.onNext("upload of ${keys.size}TEKs failed because of $e")
                Timber.e(e)
            }
        }
    }
}