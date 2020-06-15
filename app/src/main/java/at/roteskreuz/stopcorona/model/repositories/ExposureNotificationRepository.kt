package at.roteskreuz.stopcorona.model.repositories

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.managers.BluetoothManager
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.nearby.exposurenotification.*
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

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
    suspend fun isAppRegisteredForExposureNotificationsCurrentState(): Boolean

    /**
     * Retrieve the TemporaryExposureKey from the Google Exposure Notifications framework
     * in a blocking manner.
     */
    suspend fun getTemporaryExposureKeys(): List<TemporaryExposureKey>

    /**
     * Process the diagnosis key files
     */
    suspend fun processBatchDiagnosisKeys(archives: List<File>, token: String)

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
    private val exposureNotificationClient: ExposureNotificationClient
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
                if (enabled) {
                    bluetoothManager.startListeningForChanges()
                } else {
                    bluetoothManager.stopListeningForChanges()
                }
                frameworkEnabledStateSubject.onNext(enabled)
            }
            .addOnFailureListener {
                bluetoothManager.stopListeningForChanges()
                Timber.e(SilentError(it))
                frameworkEnabledStateSubject.onNext(false)
            }
    }

    override fun getExposureSettingsIntent(): Intent = Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS)

    override fun getExposureSettingsPendingIntent(context: Context): PendingIntent {
        val settingsIntent = getExposureSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, settingsIntent, PendingIntent.FLAG_ONE_SHOT)
    }

    override suspend fun isAppRegisteredForExposureNotificationsCurrentState(): Boolean {
        return suspendCancellableCoroutine { cancellableContinuation ->
            exposureNotificationClient.isEnabled
                .addOnSuccessListener {
                    cancellableContinuation.resume(it)
                }
                .addOnFailureListener {
                    Timber.e(SilentError(it))
                    cancellableContinuation.resume(false)
                }
        }
    }

    override suspend fun getTemporaryExposureKeys(): List<TemporaryExposureKey> {
        return suspendCancellableCoroutine { continuation ->
            exposureNotificationClient.temporaryExposureKeyHistory
                .addOnSuccessListener {
                    continuation.resume(it)
                }
                .addOnFailureListener {
                    continuation.cancel(it)
                }
        }
    }

    override suspend fun processBatchDiagnosisKeys(archives: List<File>, token: String) {

        val configuration = configurationRepository.getConfiguration()
            ?: throw IllegalStateException("no sense in continuing if there is not even a configuration")
        //TODO get values from configuration

        val exposureConfiguration = ExposureConfiguration.ExposureConfigurationBuilder()
            .setMinimumRiskScore(configuration.minimumRiskScore)
            //TODO check if the List can also be "spread"
            .setDurationAtAttenuationThresholds(*configuration.attenuationDurationThresholds.toIntArray())
            .setAttenuationScores(*configuration.attenuationLevelValues.toIntArray())
            .setDaysSinceLastExposureScores(*configuration.daysSinceLastExposureLevelValues.toIntArray())
            .setDurationScores(*configuration.durationLevelValues.toIntArray())
            .setTransmissionRiskScores(*configuration.transmissionRiskLevelValues.toIntArray())
            .build()

        suspendCancellableCoroutine<Unit> { continuation ->
            exposureNotificationClient.provideDiagnosisKeys(archives, exposureConfiguration, token)
                .addOnCompleteListener{
                    if (it.isSuccessful) {
                        continuation.resume(Unit)
                    } else {
                        continuation.cancel(it.exception)
                    }
                }
        }
    }

    override suspend fun determineRiskWithoutInformingUser(token: String): ExposureSummary {
        return suspendCancellableCoroutine { continuation ->
            exposureNotificationClient.getExposureSummary(token)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        continuation.resume(it.result)
                    } else {
                        continuation.cancel(it.exception)
                    }
                }
        }
    }

    override suspend fun getExposureSummaryWithPotentiallyInformingTheUser(token: String): List<ExposureInformation> {
        return suspendCancellableCoroutine { continuation ->
            exposureNotificationClient.getExposureInformation(token)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        continuation.resume(it.result)
                    } else {
                        continuation.cancel(it.exception)
                    }
                }
        }
    }
}