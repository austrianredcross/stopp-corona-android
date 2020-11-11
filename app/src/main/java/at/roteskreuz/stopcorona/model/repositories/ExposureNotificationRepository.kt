package at.roteskreuz.stopcorona.model.repositories

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.constants.Constants.ExposureNotification.EXPOSURE_ARCHIVES_FOLDER
import at.roteskreuz.stopcorona.model.entities.configuration.DbConfiguration
import at.roteskreuz.stopcorona.model.entities.session.DbBatchPart
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.managers.BluetoothManager
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.nearby.exposurenotification.*
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing Exposure notification framework.
 */
interface ExposureNotificationRepository {

    /**
     * Get information if the app is registered with the Exposure Notifications framework.
     */
    val isAppRegisteredForExposureNotificationsLastState: Boolean

    /**
     * Observe information if the app is registered with the Exposure Notifications framework.
     */
    fun observeAppIsRegisteredForExposureNotifications(): Observable<Boolean>

    /**
     * Observe if the App is registered with the Exposure Notifications Framework
     * Emit:
     * - loading when framework is starting/stopping
     * - error if happened
     * - idle otherwise
     */
    fun observeRegistrationState(): Observable<State>

    /**
     * Start automatic detecting.
     * Register receivers to handle edge situations.
     */
    fun registerAppForExposureNotifications()

    /**
     * User handled the resolution of an Error while registering the app with the Exposure
     * Notifications framework.
     */
    fun onExposureNotificationRegistrationResolutionResultOk()

    /**
     * User did not handle the resolution of an Error while registering the app with the Exposure
     * Notifications framework. Or an Error occured while handling the resolution.
     */
    fun onExposureNotificationRegistrationResolutionResultNotOk()

    /**
     * Stop automatic detection.
     * Unregister receivers of edge situations.
     */
    fun unregisterAppFromExposureNotifications()

    /**
     * Refresh the current app status regarding the observe changes in [observeAppIsRegisteredForExposureNotifications].
     */
    fun refreshExposureNotificationAppRegisteredState()

    /**
     * Get an Intent to the Exposure notification Setting in the system.
     */
    fun getExposureSettingsIntent(): Intent

    /**
     * Get a pending Intent to the Exposure notification Setting in the system.
     * Flag for new task.
     */
    fun getExposureSettingsPendingIntent(context: Context): PendingIntent

    /**
     * Get the current (refreshed) state of the Exposure Notifications Framework state.
     */
    suspend fun refreshAndGetAppRegisteredForExposureNotificationsCurrentState(): Boolean

    /**
     * Retrieve the TemporaryExposureKey from the Google Exposure Notifications framework
     * in a blocking manner.
     */
    suspend fun getTemporaryExposureKeys(): List<TemporaryExposureKey>

    /**
     * Provide the diagnosis key files of a batch to the framework
     *
     * @return True if processing has finished. False if more batches are expected to come
     */
    suspend fun provideDiagnosisKeyBatch(batches: List<DbBatchPart>, token: String): Boolean

    /**
     * Remove diagnosis key files
     */
    suspend fun removeDiagnosisKeyBatchParts(batchParts: List<DbBatchPart>)

    /**
     * use the [ExposureNotificationClient.getExposureSummary] to check if the batch is GREEN or
     * at least YELLOW
     */
    suspend fun determineRiskWithoutInformingUser(token: String): ExposureSummary

    suspend fun getExposureInformationWithPotentiallyInformingTheUser(token: String): List<ExposureInformation>
}

class ExposureNotificationRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val bluetoothManager: BluetoothManager,
    private val configurationRepository: ConfigurationRepository,
    private val exposureNotificationClient: CommonExposureClient,
    private val filesRepository: FilesRepository
) : ExposureNotificationRepository,
    CoroutineScope {

    /**
     * Holds loading or error or ok state as idle.
     */
    private val registeringWithFrameworkState = StateObserver()
    private val frameworkEnabledStateSubject = NonNullableBehaviorSubject(false)

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    override var isAppRegisteredForExposureNotificationsLastState: Boolean
        get() = frameworkEnabledStateSubject.value
        private set(value) {
            frameworkEnabledStateSubject.onNext(value)
        }

    override fun observeAppIsRegisteredForExposureNotifications(): Observable<Boolean> {
        return frameworkEnabledStateSubject
    }

    override fun observeRegistrationState(): Observable<State> {
        return registeringWithFrameworkState.observe()
    }

    override fun registerAppForExposureNotifications() {
        if (registeringWithFrameworkState.currentState is State.Loading) {
            Timber.e(SilentError("Start called when it is in loading"))
            return
        }
        registeringWithFrameworkState.loading()

        launch(coroutineContext) {

            try {
                handleFrameworkSpecificSituationOnAutoStart()
                exposureNotificationClient.start()
                refreshExposureNotificationAppRegisteredState()

                registeringWithFrameworkState.idle()
            } catch (error: Exception) {
                frameworkEnabledStateSubject.onNext(false)
                registeringWithFrameworkState.error(error)
            }

        }

    }

    override fun onExposureNotificationRegistrationResolutionResultOk() {
        registeringWithFrameworkState.loading()

        launch(coroutineContext) {
            try {
                exposureNotificationClient.start()

                refreshExposureNotificationAppRegisteredState()
                registeringWithFrameworkState.idle()

            } catch (error: Exception) {
                registeringWithFrameworkState.error(error)
            }
        }

    }

    override fun onExposureNotificationRegistrationResolutionResultNotOk() {
        refreshExposureNotificationAppRegisteredState()
        registeringWithFrameworkState.idle()
    }

    override fun unregisterAppFromExposureNotifications() {
        if (registeringWithFrameworkState.currentState is State.Loading) {
            Timber.e(SilentError("Stop called when it is in loading"))
            return
        }
        registeringWithFrameworkState.loading()

        launch(coroutineContext) {
            try {
                exposureNotificationClient.stop()

                refreshExposureNotificationAppRegisteredState()
                registeringWithFrameworkState.idle()

            } catch (error: Exception) {
                registeringWithFrameworkState.error(error)
            }

        }

    }

    override fun refreshExposureNotificationAppRegisteredState() {

        launch(coroutineContext) {
            try {
                val isServiceRunning = exposureNotificationClient.isRunning()
                updateAppRegisteredState(isServiceRunning)
            } catch (error: Exception) {
                updateAppRegisteredState(false)
                Timber.e(SilentError(error))
            }
        }

    }

    override fun getExposureSettingsIntent(): Intent = Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS)

    override fun getExposureSettingsPendingIntent(context: Context): PendingIntent {
        val settingsIntent = getExposureSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, settingsIntent, PendingIntent.FLAG_ONE_SHOT)
    }

    private fun updateAppRegisteredState(frameworkEnabled: Boolean) {
        if (frameworkEnabled) {
            bluetoothManager.startListeningForChanges()
        } else {
            bluetoothManager.stopListeningForChanges()
        }
        frameworkEnabledStateSubject.onNext(frameworkEnabled)
    }

    override suspend fun refreshAndGetAppRegisteredForExposureNotificationsCurrentState(): Boolean {
        val enabled = exposureNotificationClient.isRunning()
        updateAppRegisteredState(enabled) // update the state while reading
        return enabled
    }

    override suspend fun getTemporaryExposureKeys(): List<TemporaryExposureKey> {
        return exposureNotificationClient.getTemporaryExposureKeys()
    }

    override suspend fun provideDiagnosisKeyBatch(batches: List<DbBatchPart>, token: String): Boolean {
        val archives = batches
            .sortedBy { it.batchNumber }
            .map { filesRepository.getFile(EXPOSURE_ARCHIVES_FOLDER, it.fileName) }

        val configuration = configurationRepository.getConfiguration() ?: run {
            Timber.e(SilentError(IllegalStateException("no configuration present, failing silently")))
            return true // Processing done
        }

        val exposureConfiguration = configuration.getExposureConfiguration()

        provideDiagnosisKeys(archives, exposureConfiguration, token)
        return false // More batches to process
    }

    override suspend fun removeDiagnosisKeyBatchParts(batchParts: List<DbBatchPart>) {
        batchParts.forEach {
            filesRepository.removeFile(EXPOSURE_ARCHIVES_FOLDER, it.fileName)
        }
    }

    private suspend fun provideDiagnosisKeys(
        archives: List<File>,
        exposureConfiguration: ExposureConfiguration,
        token: String
    ) {
        exposureNotificationClient.provideDiagnosisKeys(archives, exposureConfiguration, token)
    }

    private fun DbConfiguration.getExposureConfiguration(): ExposureConfiguration {
        return ExposureConfiguration.ExposureConfigurationBuilder()
            .setMinimumRiskScore(minimumRiskScore)
            .setDurationAtAttenuationThresholds(*attenuationDurationThresholds.toIntArray())
            .setAttenuationScores(*attenuationLevelValues.toIntArray())
            .setDaysSinceLastExposureScores(*daysSinceLastExposureLevelValues.toIntArray())
            .setDurationScores(*durationLevelValues.toIntArray())
            .setTransmissionRiskScores(*transmissionRiskLevelValues.toIntArray())
            .build()
    }

    override suspend fun determineRiskWithoutInformingUser(token: String): ExposureSummary {
        return exposureNotificationClient.getExposureSummary(token)
    }

    override suspend fun getExposureInformationWithPotentiallyInformingTheUser(token: String): List<ExposureInformation> {
        return exposureNotificationClient.getExposureInformation(token)
    }
}