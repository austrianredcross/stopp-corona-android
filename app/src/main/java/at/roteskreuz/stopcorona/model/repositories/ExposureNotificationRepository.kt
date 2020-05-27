package at.roteskreuz.stopcorona.model.repositories

import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.DataState
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.StateObserver
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Repository for managing Exposure notification framework.
 */
interface ExposureNotificationRepository {

    /**
     * Observe information if framework is running or not.
     */
    fun observeIsServiceRunning(): Observable<Boolean>

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

    override fun observeIsServiceRunning(): Observable<Boolean> {
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
                val apiException: ApiException = exception
                if (apiException.statusCode == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                    frameworkState.error(apiException)
                    frameworkState.idle()
                } else {
                    frameworkState.idle()
                }
            }
            .addOnCanceledListener {
                frameworkState.idle()
            }
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