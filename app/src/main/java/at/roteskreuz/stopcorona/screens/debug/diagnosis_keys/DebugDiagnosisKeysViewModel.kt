package at.roteskreuz.stopcorona.screens.debug.diagnosis_keys

import android.app.Activity
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.api.ApiInteractor
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.repositories.ConfigurationRepository
import at.roteskreuz.stopcorona.model.repositories.DiagnosisKeysRepository
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.FilesRepository
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.screens.reporting.reportStatus.ResolutionType
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataStateObserver
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import io.reactivex.Observable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class DebugDiagnosisKeysViewModel(
    appDispatchers: AppDispatchers,
    private val apiInteractor: ApiInteractor,
    private val contextInteractor: ContextInteractor,
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val commonExposureClient: CommonExposureClient,
    private val diagnosisKeysRepository: DiagnosisKeysRepository,
    private val filesRepository: FilesRepository,
    private val configurationRepository: ConfigurationRepository
) : ScopedViewModel(appDispatchers) {

    private val exposureNotificationsEnabledSubject = NonNullableBehaviorSubject(false)
    private val exposureNotificationsTextSubject = NonNullableBehaviorSubject("no error")

    private val diagnosisKeyTokenSubject = NonNullableBehaviorSubject("no Key")

    private val exposureNotificationsErrorState = DataStateObserver<ResolutionType>()

    fun checkEnabledState() {
        launch {
            try {
                val enabled = commonExposureClient.isRunning()
                exposureNotificationsEnabledSubject.onNext(enabled)
            } catch (e : Exception) {
                Timber.e(e, "could not get the current state of the exposure notifications SDK")
                exposureNotificationsEnabledSubject.onNext(false)
                exposureNotificationsTextSubject.onNext("could not get the current state of the exposure notifications SDK: '$e'")
            }
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
    fun startExposureNotifications() {
        launch {
            try {
                exposureNotificationsErrorState.loading()
                exposureNotificationsEnabledSubject.onNext(true)
                exposureNotificationsErrorState.idle()
                exposureNotificationsTextSubject.onNext("")
            } catch (exception : Exception) {
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    exposureNotificationsEnabledSubject.onNext(false)
                    exposureNotificationsTextSubject.onNext("Unknown error when attempting to start API: '$exception'")
                    return@launch
                }
                if (exception.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    Timber.e(exception, "Error, RESOLUTION_REQUIRED in result which is not handled in UI. Framework must be running to continue.")
                    exposureNotificationsErrorState.idle()
                    exposureNotificationsTextSubject.onNext("Error, RESOLUTION_REQUIRED in result: '$exception'")
                    exposureNotificationsEnabledSubject.onNext(false)
                } else {
                    Timber.e(exception, "No RESOLUTION_REQUIRED in result")
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
                commonExposureClient.stop()
                exposureNotificationsEnabledSubject.onNext(false)
                exposureNotificationsTextSubject.onNext("app unregistered from exposure notifications")
            } catch (e : Exception) {
                exposureNotificationsEnabledSubject.onNext(true)
                Timber.w(e, "Failed to unregister")
                exposureNotificationsTextSubject.onNext("Failed to unregister from the Exposure Notifications framework: '$e'")
            }

        }
    }

    fun jumpToSystemSettings() {
        val intent = exposureNotificationRepository.getExposureSettingsIntent(contextInteractor.applicationContext)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contextInteractor.applicationContext.startActivity(intent)
    }

    fun getServicesVersion(context: Context): String {
        return commonExposureClient.getServiceVersion(context)
    }

    fun resolutionForRegistrationSucceeded(activity: Activity) {
        exposureNotificationsTextSubject.onNext("resolution succeded, trying to register again")
        startExposureNotifications()
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
                val configuration = configurationRepository.getConfiguration() ?: run {
                    Timber.e(SilentError(IllegalStateException("no configuration present, failing silently")))
                    return@launch
                }

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

                diagnosisKeysRepository.addAndUpdateKeyRequestData()
                try {
                    commonExposureClient.provideDiagnosisKeys(arrayListOf(downloadedFile), exposureConfiguration, token)
                    exposureNotificationsTextSubject.onNext("provided diagnosis keys successful with token $token")
                } catch (e : Exception) {
                    exposureNotificationsTextSubject.onNext("provided diagnosis keys not successful with token $token")
                    exposureNotificationsTextSubject.onNext("error $e")
                }
            } catch (exception: java.lang.Exception) {
                Timber.e(SilentError(exception))
                exposureNotificationsTextSubject.onNext("Error while getting the index: $exception")
            }
        }
    }

    fun getExposureSummary() {
        launch {
            try {
                exposureNotificationsTextSubject.onNext("getting the summary for: ${diagnosisKeyTokenSubject.value} ")
                val exposureSummary = commonExposureClient.getExposureSummary(diagnosisKeyTokenSubject.value)
                exposureNotificationsTextSubject.onNext("here is the summary for token ${diagnosisKeyTokenSubject.value}˜:\n  $exposureSummary")
            } catch (e : Exception) {
                exposureNotificationsTextSubject.onNext("exposure summary failed for token${diagnosisKeyTokenSubject.value}˜:\n  $e")
            }
        }
    }

    fun getDiagnosisKeysGetExposureInformation() {
        launch {
            try {
                val exposureInformation = commonExposureClient.getExposureInformation(diagnosisKeyTokenSubject.value)
                exposureNotificationsTextSubject.onNext("here is the exposure information for token${diagnosisKeyTokenSubject.value}˜:\n  $exposureInformation")
            } catch (e : Exception) {
                exposureNotificationsTextSubject.onNext("exposure information failed for token${diagnosisKeyTokenSubject.value}˜:\n  $e")
            }
        }
    }

    fun startBackgroundDiagnosisKeysProcessing() {
        launch(appDispatchers.Default) {
            try {
                exposureNotificationsTextSubject.onNext("launching the diagnosis keys background processing")
                diagnosisKeysRepository.fetchAndForwardNewDiagnosisKeysToTheExposureNotificationFramework()
                exposureNotificationsTextSubject.onNext("sucessfully launched the background processing")
            } catch (ex: Exception) {
                exposureNotificationsTextSubject.onNext("error while launching the diagnosis keys background processing: $ex")
            }

        }
    }
}
