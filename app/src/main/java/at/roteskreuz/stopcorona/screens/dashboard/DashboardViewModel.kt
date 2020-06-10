package at.roteskreuz.stopcorona.screens.dashboard

import android.app.Activity
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.managers.ChangelogManager
import at.roteskreuz.stopcorona.model.managers.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.repositories.*
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.screens.dashboard.ExposureNotificationPhase.PrerequisitesError.UnavailableGooglePlayServices.*
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.skeleton.core.utils.subscribeOnNewThread
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import at.roteskreuz.stopcorona.utils.shareReplayLast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.CancellationException

/**
 * Handles the user interaction and provides data for [DashboardFragment].
 */
class DashboardViewModel(
    appDispatchers: AppDispatchers,
    private val dashboardRepository: DashboardRepository,
    contextInteractor: ContextInteractor,
    private val infectionMessengerRepository: InfectionMessengerRepository,
    private val quarantineRepository: QuarantineRepository,
    private val configurationRepository: ConfigurationRepository,
    exposureNotificationRepository: ExposureNotificationRepository,
    private val databaseCleanupManager: DatabaseCleanupManager,
    googlePlayAvailability: GoogleApiAvailability,
    private val changelogManager: ChangelogManager,
    bluetoothRepository: BluetoothRepository
) : ScopedViewModel(appDispatchers) {

    companion object {
        const val DEFAULT_RED_WARNING_QUARANTINE = 336 // hours
        const val DEFAULT_YELLOW_WARNING_QUARANTINE = 168 // hours
    }

    /**
     * State machine which handles operations depending on the current state.
     */
    private val exposureNotificationPhaseSubject = NonNullableBehaviorSubject<ExposureNotificationPhase>(
        defaultValue = ExposureNotificationPhase.WaitingForWantedState(
            ExposureNotificationPhase.DependencyHolder(
                dashboardRepository,
                googlePlayAvailability,
                contextInteractor,
                exposureNotificationRepository,
                bluetoothRepository
            )
        )
    )

    private val exposureNotificationPhaseObservable = exposureNotificationPhaseSubject
        .subscribeOnNewThread() // needed to have sync emits
        .distinctUntilChanged()
        .shareReplayLast()

    var wasExposureFrameworkAutomaticallyEnabledOnFirstStart: Boolean
        get() = dashboardRepository.exposureFrameworkEnabledOnFirstStart
        set(value) {
            dashboardRepository.exposureFrameworkEnabledOnFirstStart = value
        }

    var userWantsToRegisterAppForExposureNotifications: Boolean
        get() = dashboardRepository.userWantsToRegisterAppForExposureNotifications
        set(value) {
            dashboardRepository.userWantsToRegisterAppForExposureNotifications = value
        }

    init {
        /**
         * If the user starts the app for the first time the exposure notification framework will be started automatically.
         */
        if (wasExposureFrameworkAutomaticallyEnabledOnFirstStart.not()) {
            wasExposureFrameworkAutomaticallyEnabledOnFirstStart = true
            userWantsToRegisterAppForExposureNotifications = true
        }

        /**
         * Handle state machine states.
         */
        disposables += observeExposureNotificationPhase()
            .subscribe { state ->
                Timber.d("Current exposure notification state is ${state.javaClass.simpleName}")
                state.onCreate { newState ->
                    state.onCleared()
                    exposureNotificationPhaseSubject.onNext(newState)
                }
            }
    }

    fun observeContactsHealthStatus(): Observable<HealthStatusData> {
        return Observables.combineLatest(
            infectionMessengerRepository.observeReceivedInfectionMessages(),
            quarantineRepository.observeQuarantineState(),
            configurationRepository.observeConfiguration()
        ).map { (infectionMessageList, quarantineStatus, configuration) ->
            val filteredInfectionMessages = infectionMessageList.filter { it.messageType != MessageType.Revoke.Suspicion }
            Triple(filteredInfectionMessages, quarantineStatus, configuration)
        }.map { (infectionMessageList, quarantineStatus, configuration) ->
            if (infectionMessageList.isNotEmpty()) {
                val redWarningQuarantineThreshold = ZonedDateTime.now().minusHours(
                    (configuration.redWarningQuarantine ?: DEFAULT_RED_WARNING_QUARANTINE).toLong()
                )
                val yellowWarningQuarantineThreshold = ZonedDateTime.now().minusHours(
                    (configuration.yellowWarningQuarantine ?: DEFAULT_YELLOW_WARNING_QUARANTINE).toLong()
                )
                HealthStatusData.ContactsSicknessInfo(
                    infectionMessageList
                        .filter { it.timeStamp > redWarningQuarantineThreshold }
                        .count { it.messageType == MessageType.InfectionLevel.Red },
                    infectionMessageList
                        .filter { it.timeStamp > yellowWarningQuarantineThreshold }
                        .count { it.messageType == MessageType.InfectionLevel.Yellow },
                    quarantineStatus
                )
            } else {
                HealthStatusData.NoHealthStatus
            }
        }
    }

    fun observeOwnHealthStatus(): Observable<HealthStatusData> {
        return quarantineRepository.observeQuarantineState()
            .map { quarantineStatus ->
                when (quarantineStatus) {
                    is QuarantineStatus.Jailed.Forever -> HealthStatusData.SicknessCertificate
                    is QuarantineStatus.Jailed.Limited -> {
                        when {
                            quarantineStatus.byContact -> HealthStatusData.NoHealthStatus
                            else -> HealthStatusData.SelfTestingSuspicionOfSickness(quarantineStatus)
                        }
                    }
                    is QuarantineStatus.Free -> {
                        when {
                            quarantineStatus.selfMonitoring -> HealthStatusData.SelfTestingSymptomsMonitoring
                            else -> HealthStatusData.NoHealthStatus
                        }
                    }
                }
            }
    }

    fun observeShowQuarantineEnd(): Observable<Boolean> {
        return quarantineRepository.observeShowQuarantineEnd()
    }

    fun quarantineEndSeen() {
        quarantineRepository.quarantineEndSeen()
    }

    fun observeSomeoneHasRecoveredStatus(): Observable<HealthStatusData> {
        return infectionMessengerRepository.observeSomeoneHasRecoveredMessage()
            .map { shouldShow ->
                if (shouldShow) {
                    HealthStatusData.SomeoneHasRecovered
                } else {
                    HealthStatusData.NoHealthStatus
                }
            }
    }

    fun someoneHasRecoveredSeen() {
        infectionMessengerRepository.someoneHasRecoveredMessageSeen()

        launch {
            databaseCleanupManager.removeReceivedGreenMessages()
        }
    }

    fun observeExposureNotificationPhase(): Observable<ExposureNotificationPhase> {
        return exposureNotificationPhaseObservable
    }

    /**
     * Handles [Activity.RESULT_OK] for a resolution. User accepted opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultOk() {
        exposureNotificationPhaseSubject.value.let { state ->
            if (state is ExposureNotificationPhase.FrameworkError.ResolutionRequired) {
                state.onResolutionOk()
            } else {
                Timber.e(SilentError("state is not RegisterActionUserApprovalNeeded when resolution is ok"))
            }
        }
    }

    /**
     * Handles not [Activity.RESULT_OK] for a resolution. User rejected opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultNotOk() {
        exposureNotificationPhaseSubject.value.let { state ->
            if (state is ExposureNotificationPhase.FrameworkError.ResolutionRequired) {
                state.onResolutionNotOk()
            } else {
                Timber.e(SilentError("state is not RegisterActionUserApprovalNeeded when resolution is not ok"))
            }
        }
    }

    fun refreshPrerequisitesErrorStatement(ignoreErrors: Boolean = false) {
        exposureNotificationPhaseSubject.value.let { state ->
            when (state) {
                is ExposureNotificationPhase.PrerequisitesError -> {
                    state.refresh()
                }
                else -> {
                    if (ignoreErrors.not()) {
                        Timber.e(SilentError("state is not PrerequisitesError"))
                    }
                }
            }
        }
    }

    override fun onCleared() {
        exposureNotificationPhaseSubject.value.onCleared()
        super.onCleared()
    }

    fun unseenChangelogForVersionAvailable(version: String) = changelogManager.unseenChangelogForVersionAvailable(version)
}

/**
 * Describes the states of the health state cards.
 */
sealed class HealthStatusData {

    /**
     * The user has successfully reported a sickness certificate to authorities.
     */
    object SicknessCertificate : HealthStatusData()

    /**
     * The user has successfully sent a self assessment to authorities with the result suspicion.
     */
    data class SelfTestingSuspicionOfSickness(
        val quarantineStatus: QuarantineStatus.Jailed.Limited
    ) : HealthStatusData()

    /**
     * The user has successfully sent a self assessment to authorities with the symptoms monitoring.
     */
    object SelfTestingSymptomsMonitoring : HealthStatusData()

    /**
     * The user has received sickness info his contacts.
     */
    class ContactsSicknessInfo(
        val confirmed: Int = 0,
        val suspicion: Int = 0,
        val quarantineStatus: QuarantineStatus
    ) : HealthStatusData()

    /**
     * Some of contacts has recovered.
     */
    object SomeoneHasRecovered : HealthStatusData()

    /**
     * No health status to be announced.
     */
    object NoHealthStatus : HealthStatusData()
}

/**
 * State machine to manage state of the exposure notification framework and checking all dependencies
 * needed for running.
 */
sealed class ExposureNotificationPhase {

    data class DependencyHolder(
        val dashboardRepository: DashboardRepository,
        val googlePlayAvailability: GoogleApiAvailability,
        val contextInteractor: ContextInteractor,
        val exposureNotificationRepository: ExposureNotificationRepository,
        val bluetoothRepository: BluetoothRepository
    )

    protected abstract val dependencyHolder: DependencyHolder

    protected val disposables = CompositeDisposable()

    abstract fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit)

    fun onCleared() {
        disposables.dispose()
    }

    /**
     * Initial state. Waiting until user clicked to enable switch button.
     */
    data class WaitingForWantedState(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            disposables += dependencyHolder.dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                .subscribeOn(Schedulers.single())
                .observeOn(Schedulers.single())
                .subscribe { wantedState ->
                    if (wantedState) {
                        moveToNextState(CheckPrerequisitesError(dependencyHolder))
                    }
                }
        }
    }

    /**
     * Checking valid google play services.
     */
    data class CheckPrerequisitesError(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                val status = googlePlayAvailability.isGooglePlayServicesAvailable(contextInteractor.applicationContext)
                val version = googlePlayAvailability.getApkVersion(contextInteractor.applicationContext)
                moveToNextState(
                    when {
                        status == ConnectionResult.SERVICE_MISSING -> {
                            ServiceMissing(dependencyHolder, googlePlayAvailability, status)
                        }
                        status == ConnectionResult.SERVICE_UPDATING -> {
                            ServiceUpdating(dependencyHolder, googlePlayAvailability, status)
                        }
                        status == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                            ServiceVersionUpdateRequired(dependencyHolder, googlePlayAvailability, status)
                        }
                        status == ConnectionResult.SERVICE_DISABLED -> {
                            ServiceDisabled(dependencyHolder, googlePlayAvailability, status)
                        }
                        status == ConnectionResult.SERVICE_INVALID -> {
                            ServiceInvalid(dependencyHolder, googlePlayAvailability, status)
                        }
                        // now status == ConnectionResult.SUCCESS
                        version < Constants.ExposureNotification.MIN_SUPPORTED_GOOGLE_PLAY_APK_VERSION -> {
                            PrerequisitesError.InvalidVersionOfGooglePlayServices(dependencyHolder)
                        }
                        bluetoothRepository.bluetoothSupported.not() -> {
                            PrerequisitesError.BluetoothNotSupported(dependencyHolder)
                        }
                        else -> {
                            RegisterToFramework(dependencyHolder, true)
                        }
                    }
                )
            }
        }
    }

    /**
     * If prerequisites not met, this is class for explaining the errors.
     */
    sealed class PrerequisitesError : ExposureNotificationPhase() {

        protected lateinit var moveToNextState: (ExposureNotificationPhase) -> Unit

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            this.moveToNextState = moveToNextState
            disposables += dependencyHolder.dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                .subscribe { wantedState ->
                    if (wantedState.not()) {
                        moveToNextState(WaitingForWantedState(dependencyHolder))
                    }
                }
        }

        /**
         * Will check prerequisites again.
         */
        fun refresh() {
            moveToNextState(CheckPrerequisitesError(dependencyHolder))
        }

        /**
         * Google play services are not available on the phone or there is some error.
         * Some of errors user can resolve.
         */
        sealed class UnavailableGooglePlayServices : PrerequisitesError() {

            abstract val googlePlayAvailability: GoogleApiAvailability
            abstract val googlePlayServicesStatusCode: Int

            data class ServiceMissing(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()

            data class ServiceUpdating(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()

            data class ServiceVersionUpdateRequired(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()

            data class ServiceDisabled(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()

            data class ServiceInvalid(
                override val dependencyHolder: DependencyHolder,
                override val googlePlayAvailability: GoogleApiAvailability,
                override val googlePlayServicesStatusCode: Int
            ) : UnavailableGooglePlayServices()
        }

        /**
         * The current Google play services version is not matching Exposure notification minimum version.
         */
        data class InvalidVersionOfGooglePlayServices(
            override val dependencyHolder: DependencyHolder
        ) : PrerequisitesError()

        /**
         * Bluetooth adapter doesn't exist or is not supported by exposure notification framework.
         */
        data class BluetoothNotSupported(
            override val dependencyHolder: DependencyHolder
        ) : PrerequisitesError()
    }

    /**
     * Registering to the exposure notification framework.
     * @param register True to start it, false to stop it.
     */
    data class RegisterToFramework(
        override val dependencyHolder: DependencyHolder,
        val register: Boolean
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .skip(1) // will skip the last value
                    .doOnSubscribe {
                        exposureNotificationRepository.refreshExposureNotificationAppRegisteredState()
                    }
                    .take(1)
                    .subscribe { currentFrameworkRegisteredState ->
                        when {
                            register -> {
                                if (currentFrameworkRegisteredState.not()) {
                                    exposureNotificationRepository.registerAppForExposureNotifications()
                                    moveToNextState(CheckingFrameworkError(dependencyHolder, register))
                                } else {
                                    moveToNextState(CheckingFrameworkRunning(dependencyHolder))
                                }
                            }
                            register.not() -> {
                                if (currentFrameworkRegisteredState) {
                                    exposureNotificationRepository.unregisterAppFromExposureNotifications()
                                    moveToNextState(CheckingFrameworkError(dependencyHolder, register))
                                } else {
                                    moveToNextState(WaitingForWantedState(dependencyHolder))
                                }
                            }
                        }
                    }
            }
        }
    }

    /**
     * Checking possible errors from registration process.
     * @param register True to start it, false to stop it.
     */
    data class CheckingFrameworkError(
        override val dependencyHolder: DependencyHolder,
        val register: Boolean
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += exposureNotificationRepository.observeRegistrationState()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { state ->
                        when (state) {
                            State.Idle -> {
                                moveToNextState(
                                    if (register) {
                                        CheckingFrameworkRunning(dependencyHolder)
                                    } else {
                                        WaitingForWantedState(dependencyHolder)
                                    }
                                )
                            }
                            is State.Error -> {
                                when (state.error) {
                                    is ApiException -> {
                                        val apiException = state.error as ApiException
                                        moveToNextState(
                                            when (apiException.statusCode) {
                                                CommonStatusCodes.SIGN_IN_REQUIRED -> {
                                                    Timber.e(SilentError("SIGN_IN_REQUIRED", apiException))
                                                    FrameworkError.SignInRequired(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.INVALID_ACCOUNT -> {
                                                    Timber.e(SilentError("INVALID_ACCOUNT", apiException))
                                                    FrameworkError.InvalidAccount(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.RESOLUTION_REQUIRED -> {
                                                    // no logging of error, this state is expect-able
                                                    FrameworkError.ResolutionRequired(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.NETWORK_ERROR -> {
                                                    Timber.e(SilentError("NETWORK_ERROR", apiException))
                                                    FrameworkError.NetworkError(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.INTERNAL_ERROR -> {
                                                    Timber.e(SilentError("INTERNAL_ERROR", apiException))
                                                    FrameworkError.InternalError(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.DEVELOPER_ERROR -> {
                                                    Timber.e(SilentError("DEVELOPER_ERROR", apiException))
                                                    FrameworkError.DeveloperError(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.ERROR -> {
                                                    Timber.e(SilentError("ERROR", apiException))
                                                    FrameworkError.Error(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.INTERRUPTED -> {
                                                    Timber.e(SilentError("INTERRUPTED", apiException))
                                                    FrameworkError.Interrupted(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.TIMEOUT -> {
                                                    Timber.e(SilentError("TIMEOUT", apiException))
                                                    FrameworkError.Timeout(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.CANCELED -> {
                                                    Timber.e(SilentError("CANCELED", apiException))
                                                    FrameworkError.Canceled(dependencyHolder, apiException, register)
                                                }
                                                CommonStatusCodes.API_NOT_CONNECTED -> {
                                                    Timber.e(SilentError("API_NOT_CONNECTED", apiException))
                                                    FrameworkError.ApiNotConnected(dependencyHolder, apiException, register)
                                                }
                                                else -> {
                                                    FrameworkError.Unknown(dependencyHolder, apiException, register)
                                                }
                                            }
                                        )
                                    }
                                    is CancellationException -> {
                                        moveToNextState(WaitingForWantedState(dependencyHolder))
                                    }
                                    else -> {
                                        moveToNextState(FrameworkError.Unknown(dependencyHolder, state.error, register))
                                    }
                                }
                            }
                        }
                    }
                disposables += dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { wantedState ->
                        if (wantedState.not()) {
                            moveToNextState(WaitingForWantedState(dependencyHolder))
                        }
                    }
            }
        }
    }

    /**
     * Errors from exposure notification framework.
     */
    sealed class FrameworkError : ExposureNotificationPhase() {

        protected lateinit var moveToNextState: (ExposureNotificationPhase) -> Unit
        protected abstract val register: Boolean

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            this.moveToNextState = moveToNextState
            disposables += dependencyHolder.dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                .subscribeOn(Schedulers.single())
                .observeOn(Schedulers.single())
                .subscribe { wantedState ->
                    if (wantedState.not()) {
                        moveToNextState(RegisterToFramework(dependencyHolder, false))
                    }
                }
        }

        /**
         * Will refresh exposure notification registration state again.
         */
        open fun refresh() {
            moveToNextState(RegisterToFramework(dependencyHolder, register))
        }

        /**
         * Framework [ApiException] of [CommonStatusCodes.SIGN_IN_REQUIRED].
         */
        data class SignInRequired(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.INVALID_ACCOUNT].
         */
        data class InvalidAccount(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.RESOLUTION_REQUIRED].
         * User must confirm the registration app to the framework.
         */
        data class ResolutionRequired(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError() {

            fun onResolutionOk() {
                dependencyHolder.exposureNotificationRepository.onExposureNotificationRegistrationResolutionResultOk()
                moveToNextState(CheckingFrameworkError(dependencyHolder, register))
            }

            fun onResolutionNotOk() {
                dependencyHolder.exposureNotificationRepository.onExposureNotificationRegistrationResolutionResultNotOk()
                moveToNextState(ResolutionDeclined(dependencyHolder, register))
            }
        }

        /**
         * Registration dialog to the framework was declined by user.
         */
        data class ResolutionDeclined(
            override val dependencyHolder: DependencyHolder,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.NETWORK_ERROR].
         */
        data class NetworkError(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.INTERNAL_ERROR].
         */
        data class InternalError(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.DEVELOPER_ERROR].
         */
        data class DeveloperError(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.ERROR].
         */
        data class Error(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.INTERRUPTED].
         */
        data class Interrupted(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.TIMEOUT].
         */
        data class Timeout(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.CANCELED].
         */
        data class Canceled(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework [ApiException] of [CommonStatusCodes.API_NOT_CONNECTED].
         */
        data class ApiNotConnected(
            override val dependencyHolder: DependencyHolder,
            val exception: ApiException,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Framework caused some unknown error.
         * @param register True to start it, false to stop it.
         */
        data class Unknown(
            override val dependencyHolder: DependencyHolder,
            val exception: Throwable,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Bluetooth is not enabled.
         */
        data class BluetoothNotEnabled(
            override val dependencyHolder: DependencyHolder
        ) : FrameworkError() {

            override val register: Boolean
                get() = throw IllegalAccessException("Not used in this context")

            override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
                super.onCreate(moveToNextState)
                disposables += dependencyHolder.bluetoothRepository.observeBluetoothEnabledState()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { enabled ->
                        if (enabled) {
                            moveToNextState(CheckingFrameworkRunning(dependencyHolder))
                        }
                    }
            }
        }
    }

    /**
     * Checking if framework is registered and running.
     */
    data class CheckingFrameworkRunning(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += bluetoothRepository.observeBluetoothEnabledState()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { enabled ->
                        if (enabled.not()) {
                            moveToNextState(FrameworkError.BluetoothNotEnabled(dependencyHolder))
                        }
                    }
                disposables += exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { realState ->
                        if (realState) {
                            moveToNextState(FrameworkRunning(dependencyHolder))
                        }
                    }
                disposables += dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { wantedState ->
                        if (wantedState.not()) {
                            moveToNextState(RegisterToFramework(dependencyHolder, false))
                        }
                    }
            }
        }
    }

    /**
     * Framework is registered and running.
     */
    data class FrameworkRunning(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += bluetoothRepository.observeBluetoothEnabledState()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { enabled ->
                        if (enabled.not()) {
                            moveToNextState(FrameworkError.BluetoothNotEnabled(dependencyHolder))
                        }
                    }
                disposables += dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { wantedState ->
                        if (wantedState.not()) {
                            moveToNextState(RegisterToFramework(dependencyHolder, false))
                        }
                    }
                disposables += exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.single())
                    .subscribe { realState ->
                        if (realState.not()) {
                            moveToNextState(RegisterToFramework(dependencyHolder, false))
                        }
                    }
            }
        }
    }
}