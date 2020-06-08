package at.roteskreuz.stopcorona.model.repositories

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.receivers.BluetoothStateReceiver
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.tasks.Task
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
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
}

class ExposureNotificationRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val bluetoothStateReceiver: BluetoothStateReceiver,
    private val exposureNotificationClient: ExposureNotificationClient,
    private val contextInteractor: ContextInteractor
) : ExposureNotificationRepository,
    CoroutineScope {

    /**
     * Holds the enabled state, loading or error.
     */
    private val registeringWithFrameworkState = StateObserver()
    private val frameworkEnabledState = NonNullableBehaviorSubject(false)

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    override var isAppRegisteredForExposureNotificationsLastState: Boolean
        get() = frameworkEnabledState.value
        private set(value) {
            frameworkEnabledState.onNext(value)
        }

    override fun observeAppIsRegisteredForExposureNotifications(): Observable<Boolean> {
        return frameworkEnabledState
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
                refreshExposureNotificationAppRegisteredState()
                registeringWithFrameworkState.idle()
                bluetoothStateReceiver.register(contextInteractor.applicationContext)
            }
            .addOnFailureListener { exception: Exception ->
                registeringWithFrameworkState.error(exception)
                bluetoothStateReceiver.unregisterFailSilent(contextInteractor.applicationContext)
            }
            .addOnCanceledListener {
                registeringWithFrameworkState.idle()
                bluetoothStateReceiver.unregisterFailSilent(contextInteractor.applicationContext)
            }
    }

    override fun onExposureNotificationRegistrationResolutionResultOk() {
        registeringWithFrameworkState.loading()
        exposureNotificationClient.stop()
            .addOnSuccessListener {
                refreshExposureNotificationAppRegisteredState()
                registeringWithFrameworkState.idle()
                bluetoothStateReceiver.register(contextInteractor.applicationContext)
            }
            .addOnFailureListener { exception: Exception ->
                registeringWithFrameworkState.error(exception)
                bluetoothStateReceiver.unregisterFailSilent(contextInteractor.applicationContext)
            }
            .addOnCanceledListener {
                registeringWithFrameworkState.idle()
                bluetoothStateReceiver.unregisterFailSilent(contextInteractor.applicationContext)
            }
    }

    override fun onExposureNotificationRegistrationResolutionResultNotOk() {
        registeringWithFrameworkState.idle()
        frameworkEnabledState.onNext(false)
        bluetoothStateReceiver.unregisterFailSilent(contextInteractor.applicationContext)
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
                bluetoothStateReceiver.unregisterFailSilent(contextInteractor.applicationContext)
            }
            .addOnFailureListener { exception: Exception ->
                registeringWithFrameworkState.error(exception)
                bluetoothStateReceiver.register(contextInteractor.applicationContext)
            }
    }

    override fun refreshExposureNotificationAppRegisteredState() {
        exposureNotificationClient.isEnabled
            .addOnSuccessListener { enabled: Boolean ->
                frameworkEnabledState.onNext(enabled)
            }
            .addOnFailureListener {
                Timber.e(SilentError(it))
                frameworkEnabledState.onNext(false)
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
                    cancellableContinuation.resume(false)
                }
        }
    }

    private fun Task<Void>.sendResultTo(stateObserver: StateObserver) {
        addOnSuccessListener {
            refreshExposureNotificationAppRegisteredState()
            stateObserver.idle()
        }.addOnFailureListener { exception: Exception ->
            stateObserver.error(exception)
        }.addOnCanceledListener {
            stateObserver.idle()
        }
    }
}