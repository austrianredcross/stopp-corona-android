package at.roteskreuz.stopcorona.model.repositories

import android.app.Activity
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
     * Get information if framework is running or not.
     */
    val isFrameworkRunning: Boolean

    /**
     * Observe information if framework is running or not.
     */
    fun observeIsFrameworkRunning(): Observable<Boolean>

    /**
     * Observe information about starting/stopping and error.
     * Emit:
     * - loading when framework is starting/stopping
     * - error if happened
     * - idle otherwise
     */
    fun observeState(): Observable<State>

    /**
     * Start automatic detecting.
     * Register receivers to handle edge situations.
     */
    fun startListening()

    /**
     * Handles [Activity.RESULT_OK] for a resolution. User accepted opt-in.
     */
    fun startResolutionResultOk()

    /**
     * Handles not [Activity.RESULT_OK] for a resolution. User accepted opt-in.
     */
    fun startResolutionResultNotOk()

    /**
     * Stop automatic detection.
     * Unregister receivers of edge situations.
     */
    fun stopListening()
}

class ExposureNotificationRepositoryImpl(
    private val appDispatchers: AppDispatchers,
    private val exposureNotificationClient: ExposureNotificationClient
) : ExposureNotificationRepository,
    CoroutineScope {

    /**
     * Holds the enabled state, loading or error.
     */
    private val frameworkState = StateObserver()
    private val frameworkEnabledState = NonNullableBehaviorSubject(false)

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    override var isFrameworkRunning: Boolean
        get() = frameworkEnabledState.value
        private set(value) {
            frameworkEnabledState.onNext(value)
        }

    override fun observeIsFrameworkRunning(): Observable<Boolean> {
        return frameworkEnabledState
    }

    override fun observeState(): Observable<State> {
        return frameworkState.observe()
    }

    override fun startListening() {
        if (frameworkState.currentState is State.Loading) {
            Timber.e(SilentError("Start called when it is in loading"))
            return
        }
        frameworkState.loading()
        exposureNotificationClient.start()
            .addOnSuccessListener {
                refreshEnabledState()
                frameworkState.idle()
            }
            .addOnFailureListener { exception: Exception ->
                if (exception !is ApiException) {
                    Timber.e(exception, "Unknown error when attempting to start API")
                    frameworkState.idle()
                    return@addOnFailureListener
                }
                frameworkState.error(exception) // will be type of ApiException
                frameworkState.idle()
            }
            .addOnCanceledListener {
                frameworkState.idle()
            }
    }

    override fun startResolutionResultOk() {
        frameworkState.loading()
        exposureNotificationClient.start()
            .addOnSuccessListener {
                refreshEnabledState()
                frameworkState.idle()
            }
            .addOnFailureListener { exception: Exception ->
                Timber.e(exception, "Error handling resolution ok")
                frameworkState.idle()
            }
            .addOnCanceledListener {
                frameworkState.idle()
            }
    }

    override fun startResolutionResultNotOk() {
        frameworkState.idle()
        frameworkEnabledState.onNext(false)
    }

    override fun stopListening() {
        if (frameworkState.currentState is State.Loading) {
            Timber.e(SilentError("Stop called when it is in loading"))
            return
        }
        frameworkState.loading()
        exposureNotificationClient.start()
            .addOnSuccessListener {
                refreshEnabledState()
                frameworkState.idle()
            }
            .addOnFailureListener { exception: Exception ->
                Timber.e(exception, "Unknown error when attempting to start API")
                frameworkState.idle()
            }
    }

    private fun refreshEnabledState() {
        exposureNotificationClient.isEnabled
            .addOnSuccessListener { enabled: Boolean ->
                frameworkEnabledState.onNext(enabled)
            }
    }
}