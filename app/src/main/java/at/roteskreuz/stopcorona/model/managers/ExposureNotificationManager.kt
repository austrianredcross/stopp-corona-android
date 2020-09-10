package at.roteskreuz.stopcorona.model.managers

import android.app.Activity
import android.content.SharedPreferences
import androidx.work.WorkManager
import at.roteskreuz.stopcorona.commonexposure.CommonExposureClient
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase.PrerequisitesError
import at.roteskreuz.stopcorona.model.repositories.BluetoothRepository
import at.roteskreuz.stopcorona.model.repositories.ExposureNotificationRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.model.workers.ExposureMatchingWorker
import at.roteskreuz.stopcorona.model.workers.UploadMissingExposureKeysReminderWorker
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.utils.booleanSharedPreferencesProperty
import at.roteskreuz.stopcorona.skeleton.core.utils.observeBoolean
import at.roteskreuz.stopcorona.skeleton.core.utils.subscribeOnNewThread
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import at.roteskreuz.stopcorona.utils.shareReplayLast
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Manager to handle state of [ExposureNotificationRepository].
 */
interface ExposureNotificationManager {

    /**
     * Get the current exposure phase.
     */
    val currentPhase: ExposureNotificationPhase

    /**
     * Indicate a user intent to register.
     * True still doesn't mean, that the framework is successfully registered,
     * use [ExposureNotificationRepository.isAppRegisteredForExposureNotificationsLastState] instead.
     */
    var userWantsToRegisterAppForExposureNotifications: Boolean

    /**
     * Indicate a user intent to register.
     * True still doesn't mean, that the framework is successfully registered,
     * use [ExposureNotificationRepository.isAppRegisteredForExposureNotificationsLastState] instead.
     */
    fun observeUserWantsToRegisterAppForExposureNotification(): Observable<Boolean>

    fun observeExposureNotificationPhase(): Observable<ExposureNotificationPhase>

    /**
     * Handles [Activity.RESULT_OK] for a resolution. User accepted opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultOk()

    /**
     * Handles not [Activity.RESULT_OK] for a resolution. User rejected opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultNotOk()

    /**
     * Check prerequisites again.
     * @param ignoreErrors If true and the current state is not [PrerequisitesError], error will be logged.
     */
    fun refreshPrerequisitesErrorStatement(ignoreErrors: Boolean = false)
}

class ExposureNotificationManagerImpl(
    private val appDispatchers: AppDispatchers,
    private val preferences: SharedPreferences,
    exposureNotificationRepository: ExposureNotificationRepository,
    exposureClient: CommonExposureClient,
    bluetoothRepository: BluetoothRepository,
    contextInteractor: ContextInteractor,
    quarantineRepository: QuarantineRepository,
    workManager: WorkManager
) : ExposureNotificationManager,
    CoroutineScope {

    companion object {
        private const val PREF_WANTED_STATE_OF_APP_EXPOSURE_NOTIFICATION_REGISTRATION =
            Constants.Prefs.EXPOSURE_NOTIFICATION_MANAGER_PREFIX + "wanted_state_of_app_exposure_notification_registration"
    }

    /**
     * State machine which handles operations depending on the current state.
     */
    private val exposureNotificationPhaseSubject = NonNullableBehaviorSubject<ExposureNotificationPhase>(
        defaultValue = ExposureNotificationPhase.WaitingForWantedState(
            ExposureNotificationPhase.DependencyHolder(
                exposureClient,
                this,
                contextInteractor,
                exposureNotificationRepository,
                bluetoothRepository
            )
        )
    )

    private val phaseObservable = exposureNotificationPhaseSubject
        .subscribeOnNewThread() // needed to have sync emits
        .distinctUntilChanged()
        .shareReplayLast()

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    override val currentPhase: ExposureNotificationPhase
        get() = exposureNotificationPhaseSubject.value

    override var userWantsToRegisterAppForExposureNotifications: Boolean
            by preferences.booleanSharedPreferencesProperty(PREF_WANTED_STATE_OF_APP_EXPOSURE_NOTIFICATION_REGISTRATION, false)

    init {
        /**
         * Live handling state machine states.
         */
        @Suppress("CheckResult")
        phaseObservable
            .subscribe { state ->
                Timber.d("Current exposure notification state is ${state.javaClass.simpleName}")

                when (state) {
                    is ExposureNotificationPhase.FrameworkRunning -> {
                        // enqueue the periodic work request to run the exposure matching algorithm when framework is running
                        // if the periodic work is already scheduled, nothing will happen
                        // this ensures to check exposure matching only if user started the framework at least once
                        ExposureMatchingWorker.enqueueExposurePeriodicMatching(workManager)
                    }
                }

                state.onCreate { newState ->
                    state.onCleared()
                    exposureNotificationPhaseSubject.onNext(newState)
                }
            }
        @Suppress("CheckResult")
        quarantineRepository.observeIfUploadOfMissingExposureKeysIsNeeded()
            .subscribe {
                val uploadMissingExposureKeys = it.orElse(null)
                if (uploadMissingExposureKeys != null) {
                    UploadMissingExposureKeysReminderWorker.enqueueUploadMissingExposureKeysWorker(workManager, uploadMissingExposureKeys.uploadAfter)
                } else {
                    UploadMissingExposureKeysReminderWorker.cancelUploadMissingExposureKeysWorker(workManager)
                }
            }
    }

    override fun observeUserWantsToRegisterAppForExposureNotification(): Observable<Boolean> {
        return preferences.observeBoolean(PREF_WANTED_STATE_OF_APP_EXPOSURE_NOTIFICATION_REGISTRATION, false)
    }

    override fun observeExposureNotificationPhase(): Observable<ExposureNotificationPhase> {
        return phaseObservable
    }

    override fun onExposureNotificationRegistrationResolutionResultOk() {
        currentPhase.let { phase ->
            if (phase is ExposureNotificationPhase.FrameworkError.Critical.ResolutionRequired) {
                phase.onResolutionOk()
            } else {
                Timber.e(SilentError("state is not RegisterActionUserApprovalNeeded when resolution is ok"))
            }
        }
    }

    override fun onExposureNotificationRegistrationResolutionResultNotOk() {
        currentPhase.let { phase ->
            if (phase is ExposureNotificationPhase.FrameworkError.Critical.ResolutionRequired) {
                phase.onResolutionNotOk()
            } else {
                Timber.e(SilentError("state is not RegisterActionUserApprovalNeeded when resolution is not ok"))
            }
        }
    }

    override fun refreshPrerequisitesErrorStatement(ignoreErrors: Boolean) {
        currentPhase.let { phase ->
            when (phase) {
                is PrerequisitesError -> {
                    phase.refresh()
                }
                else -> {
                    if (ignoreErrors.not()) {
                        Timber.e(SilentError("state is not PrerequisitesError"))
                    }
                }
            }
        }
    }
}

