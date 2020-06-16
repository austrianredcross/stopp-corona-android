package at.roteskreuz.stopcorona.model.repositories

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.model.entities.session.DbFullBatchPart
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.managers.BluetoothManager
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.nearby.exposurenotification.*
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.CancellationException
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
     * Process the diagnosis key files
     */
    suspend fun processBatchDiagnosisKeys(archives: List<DbFullBatchPart>, token: String)

    /**
     * use the [ExposureNotificationClient.getExposureSummary] to check if the batch is GREEN or
     * at least YELLOW
     */
    suspend fun determineRiskWithoutInformingUser(token: String): ExposureSummary

    suspend fun getExposureSummaryWithPotentiallyInformingTheUser(token: String): List<ExposureInformation>
}

class ExposureNotificationRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val bluetoothManager: BluetoothManager,
    private val configurationRepository: ConfigurationRepository,
    private val exposureNotificationClient: ExposureNotificationClient,
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
        exposureNotificationClient.start()
            .addOnSuccessListener {
                registeringWithFrameworkState.idle()
            }
            .addOnFailureListener { exception: Exception ->
                frameworkEnabledStateSubject.onNext(false)
                registeringWithFrameworkState.error(exception)
            }
            .addOnCanceledListener {
                registeringWithFrameworkState.error(CancellationException())
            }
    }

    override fun onExposureNotificationRegistrationResolutionResultOk() {
        registeringWithFrameworkState.loading()
        exposureNotificationClient.start()
            .addOnSuccessListener {
                refreshExposureNotificationAppRegisteredState()
                registeringWithFrameworkState.idle()
            }
            .addOnFailureListener { exception: Exception ->
                registeringWithFrameworkState.error(exception)
            }
            .addOnCanceledListener {
                registeringWithFrameworkState.error(CancellationException())
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
        exposureNotificationClient.stop()
            .addOnSuccessListener {
                refreshExposureNotificationAppRegisteredState()
                registeringWithFrameworkState.idle()
            }
            .addOnFailureListener { exception: Exception ->
                registeringWithFrameworkState.error(exception)
            }
    }

    override fun refreshExposureNotificationAppRegisteredState() {
        exposureNotificationClient.isEnabled
            .addOnSuccessListener { enabled: Boolean ->
                updateAppRegisteredState(enabled)
            }
            .addOnFailureListener {
                updateAppRegisteredState(false)
                Timber.e(SilentError(it))
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
        val enabled = exposureNotificationClient.isEnabled.await()
        updateAppRegisteredState(enabled) // update the state while reading
        return enabled
    }

    override suspend fun getTemporaryExposureKeys(): List<TemporaryExposureKey> {
        return exposureNotificationClient.temporaryExposureKeyHistory.await()
    }

    override suspend fun processBatchDiagnosisKeys(batches: List<DbFullBatchPart>, token: String) {
        val archives = batches
            .sortedWith(compareBy { it.batchNumber })
            .map { filesRepository.getFile(it.fileName) }

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

        exposureNotificationClient.provideDiagnosisKeys(archives, exposureConfiguration, token).await()
    }

    override suspend fun determineRiskWithoutInformingUser(token: String): ExposureSummary {
        return exposureNotificationClient.getExposureSummary(token).await()
    }

    override suspend fun getExposureSummaryWithPotentiallyInformingTheUser(token: String): List<ExposureInformation> {
        return exposureNotificationClient.getExposureInformation(token).await()
    }
}