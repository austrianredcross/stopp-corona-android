package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing Exposure notification framework.
 */
interface ExposureNotificationRepository {

    /**
     * Get information if the app is registered with the Exposure Notifications framework.
     */
    val isAppRegisteredForExposureNotifications: Boolean

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
}

class ExposureNotificationRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val exposureNotificationClient: ExposureNotificationClient
) : ExposureNotificationRepository,
    CoroutineScope {

    /**
     * Holds the enabled state, loading or error.
     */
    private val registeringWithFrameworkState = StateObserver()
    private val frameworkEnabledState = NonNullableBehaviorSubject(false)

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    override var isAppRegisteredForExposureNotifications: Boolean
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
            }
            .addOnFailureListener { exception: Exception ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    registeringWithFrameworkState.idle()
                    return@addOnFailureListener
                }
                registeringWithFrameworkState.error(exception) // will be type of ApiException
                registeringWithFrameworkState.idle()
            }
            .addOnCanceledListener {
                registeringWithFrameworkState.idle()
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
                Timber.e(exception, "Error handling resolution ok")
                registeringWithFrameworkState.idle()
            }
            .addOnCanceledListener {
                registeringWithFrameworkState.idle()
            }
    }

    override fun onExposureNotificationRegistrationResolutionResultNotOk() {
        registeringWithFrameworkState.idle()
        frameworkEnabledState.onNext(false)
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
                Timber.e(exception, "Unknown error when attempting to start API")
                registeringWithFrameworkState.idle()
            }
    }

    override fun refreshExposureNotificationAppRegisteredState() {
        exposureNotificationClient.isEnabled
            .addOnSuccessListener { enabled: Boolean ->
                frameworkEnabledState.onNext(enabled)
            }
    }
}