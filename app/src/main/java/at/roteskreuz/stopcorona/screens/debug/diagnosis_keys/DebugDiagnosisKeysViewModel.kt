package at.roteskreuz.stopcorona.screens.debug.diagnosis_keys

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.ConfigurationRepository
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.FilesRepository
import at.roteskreuz.stopcorona.model.repositories.InfectionMessengerRepository
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ResolutionType
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import io.reactivex.Observable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

class DebugDiagnosisKeysViewModel(
    appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val contextInteractor: ContextInteractor,
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val exposureNotificationClient: ExposureNotificationClient,
    private val infectionMessengerRepository: InfectionMessengerRepository,
    private val filesRepository: FilesRepository,
    private val configurationRepository: ConfigurationRepository
) : ScopedViewModel(appDispatchers) {

    private val exposureNotificationsEnabledSubject = NonNullableBehaviorSubject(false)
    private val exposureNotificationsTextSubject = NonNullableBehaviorSubject("no error")

    private val diagnosisKeyTokenSubject = NonNullableBehaviorSubject("no Key")

    private val exposureNotificationsErrorState = DataStateObserver<ResolutionType>()

    fun checkEnabledState() {
        exposureNotificationClient.isEnabled
            .addOnSuccessListener { enabled: Boolean ->
                exposureNotificationsEnabledSubject.onNext(enabled)
            }
            .addOnFailureListener { exception: Exception? ->
                Timber.e(exception, "could not get the current state of the exposure notifications SDK")
                exposureNotificationsEnabledSubject.onNext(false)
                exposureNotificationsTextSubject.onNext("could not get the current state of the exposure notifications SDK: '$exception'")
            }
    }

    fun observeEnabledState(): Observable<Boolean> {
        return exposureNotificationsEnabledSubject
    }

    fun observeResolutionError(): Observable<DataState<ResolutionType>> {
        return exposureNotificationsErrorState.observe()
    }

    fun observeResolutionErrorReasons(): Observable<String> {
        return exposureNotificationsTextSubject
    }

    fun observeDiagnosisKeyToken(): Observable<String> {
        return diagnosisKeyTokenSubject
    }

    /**
     * Calls start on the Exposure Notifications API.
     */
    fun startExposureNotifications(activity: Activity) {
        exposureNotificationsErrorState.loading()
        exposureNotificationClient.start()
            .addOnSuccessListener {
                exposureNotificationsEnabledSubject.onNext(true)
                exposureNotificationsErrorState.idle()
                exposureNotificationsTextSubject.onNext("")
            }
            .addOnFailureListener { exception: Exception? ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsEnabledSubject.onNext(false)
                    exposureNotificationsTextSubject.onNext("Unknown error when attempting to start API: '$exception'")
                    return@addOnFailureListener
                }
                val apiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result which is not handled in UI. Framework must be running to continue.")
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
            .addOnSuccessListener {
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
        val intent = exposureNotificationRepository.getExposureSettingsIntent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contextInteractor.applicationContext.startActivity(intent)
    }

    fun googlePlayServicesVersion(): String {
        return contextInteractor.applicationContext.getString(
            R.string.debug_version_gms,
            getVersionNameForPackage(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE)
        )
    }

    /**
     * Gets the version name for a specified package. Returns a debug string if not found.
     */
    private fun getVersionNameForPackage(packageName: String): String? {
        try {
            return contextInteractor.applicationContext.packageManager
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

    fun resolutionForRegistrationFailed(resultCode: Int) {
        exposureNotificationsTextSubject.onNext("resolutionForRegistrationFailed with code: $resultCode")
    }

    fun downloadDiagnosisKeysArchiveIndex() {
        launch(appDispatchers.Default) {
            try {
                exposureNotificationsTextSubject.onNext("downloading the index now")
                val archive = apiInteractor.getIndexOfDiagnosisKeysArchives()
                val pathToFirstArchive = archive.full14DaysBatch.batchFilePaths.first()
                exposureNotificationsTextSubject.onNext("got the archive $archive now downloading $pathToFirstArchive")

                delay(1000)

                val fileName = apiInteractor.downloadContentDeliveryFile(pathToFirstArchive)
                val downloadedFile = filesRepository.getFile(Constants.ExposureNotification.EXPOSURE_ARCHIVES_FOLDER, fileName)
                exposureNotificationsTextSubject.onNext("$pathToFirstArchive downloaded successfully to " +
                    "${downloadedFile.absolutePath}} resulting in a filesize of ${downloadedFile.length()} bytes  ")

                delay(1000)

                exposureNotificationsTextSubject.onNext("providing diagnosis keys")
                val configuration = configurationRepository.getConfiguration()
                    ?: throw IllegalStateException("no sense in continuing if there is not even a configuration")

                val exposureConfiguration = ExposureConfiguration.ExposureConfigurationBuilder()
                    .setMinimumRiskScore(configuration.minimumRiskScore)
                    .setDurationAtAttenuationThresholds(*configuration.attenuationDurationThresholds.toIntArray())
                    .setAttenuationScores(*configuration.attenuationLevelValues.toIntArray())
                    .setDaysSinceLastExposureScores(*configuration.daysSinceLastExposureLevelValues.toIntArray())
                    .setDurationScores(*configuration.durationLevelValues.toIntArray())
                    .setTransmissionRiskScores(*configuration.transmissionRiskLevelValues.toIntArray())
                    .build()
                val token = UUID.randomUUID().toString()
                diagnosisKeyTokenSubject.onNext(token)

                delay(1000)

                exposureNotificationClient.provideDiagnosisKeys(arrayListOf(downloadedFile), exposureConfiguration, token)
                    .addOnCompleteListener {
                        exposureNotificationsTextSubject.onNext(
                            "provided diagnosis keys ${if (it.isSuccessful) "successful" else "not successful"} with token $token")
                        if (it.isSuccessful.not()) {
                            exposureNotificationsTextSubject.onNext("error ${it.exception}")
                        }
                    }
            } catch (exception: java.lang.Exception) {
                Timber.e(SilentError(exception))
                exposureNotificationsTextSubject.onNext("Error while getting the index: $exception")
            }
        }
    }

    fun getExposureSummary() {
        launch {
            exposureNotificationsTextSubject.onNext("getting the summary for: ${diagnosisKeyTokenSubject.value} ")
            exposureNotificationClient.getExposureSummary(diagnosisKeyTokenSubject.value)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        exposureNotificationsTextSubject.onNext("here is the summary for token ${diagnosisKeyTokenSubject.value}˜:\n  ${it.result}")
                    } else {
                        exposureNotificationsTextSubject.onNext(
                            "exposure summary failed for token${diagnosisKeyTokenSubject.value}˜:\n  ${it.exception}")
                    }
                }
        }
    }

    fun getDiagnosisKeysGetExposureInformation() {
        launch {
            exposureNotificationClient.getExposureInformation(diagnosisKeyTokenSubject.value)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        exposureNotificationsTextSubject.onNext(
                            "here is the exposure information for token${diagnosisKeyTokenSubject.value}˜:\n  ${it.result}")
                    } else {
                        exposureNotificationsTextSubject.onNext(
                            "exposure information failed for token${diagnosisKeyTokenSubject.value}˜:\n  ${it.exception}")
                    }
                }
        }
    }

    fun startBackgroundDiagnosisKeysProcessing() {
        launch(appDispatchers.Default) {
            try {
                exposureNotificationsTextSubject.onNext("launching the diagnosis keys background processing")
                infectionMessengerRepository.fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()
                exposureNotificationsTextSubject.onNext("sucessfully launched the background processing")
            } catch (ex: Exception) {
                exposureNotificationsTextSubject.onNext("error while launching the diagnosis keys background processing: $ex")
            }

        }
    }
}
