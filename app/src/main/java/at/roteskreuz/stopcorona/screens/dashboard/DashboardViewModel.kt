package at.roteskreuz.stopcorona.screens.dashboard

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.model.manager.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.repositories.*
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import at.roteskreuz.stopcorona.utils.shareReplayLast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

/**
 * Handles the user interaction and provides data for [DashboardFragment].
 */
class DashboardViewModel(
    appDispatchers: AppDispatchers,
    private val dashboardRepository: DashboardRepository,
    private val contextInteractor: ContextInteractor,
    private val infectionMessengerRepository: InfectionMessengerRepository,
    private val quarantineRepository: QuarantineRepository,
    private val configurationRepository: ConfigurationRepository,
    private val exposureNotificationRepository: ExposureNotificationRepository,
    private val databaseCleanupManager: DatabaseCleanupManager,
    private val googlePlayAvailability: GoogleApiAvailability
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
                exposureNotificationRepository
            )
        )
    )

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
                Timber.w("Exposure notification phase = ${state.javaClass.simpleName}")
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
        return exposureNotificationPhaseSubject
            .observeOn(Schedulers.newThread()) // needed to have synchronised emits
            .distinctUntilChanged()
            .shareReplayLast()
    }

    /**
     * Handles [Activity.RESULT_OK] for a resolution. User accepted opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultOk() {
        exposureNotificationPhaseSubject.value.let { state ->
            if (state is ExposureNotificationPhase.RegisterActionUserApprovalNeeded) {
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
            if (state is ExposureNotificationPhase.RegisterActionUserApprovalNeeded) {
                state.onResolutionNotOk()
            } else {
                Timber.e(SilentError("state is not RegisterActionUserApprovalNeeded when resolution is not ok"))
            }
        }
    }

    override fun onCleared() {
        exposureNotificationPhaseSubject.value.onCleared()
        super.onCleared()
    }
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

sealed class ExposureNotificationPhase {

    data class DependencyHolder(
        val dashboardRepository: DashboardRepository,
        val googlePlayAvailability: GoogleApiAvailability,
        val contextInteractor: ContextInteractor,
        val exposureNotificationRepository: ExposureNotificationRepository
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
                when {
                    googlePlayAvailability.isGooglePlayServicesAvailable(contextInteractor.applicationContext) != ConnectionResult.SUCCESS -> {
                        moveToNextState(PrerequisitesError.UnavailableGooglePlayServices(dependencyHolder))
                    }
                    // TODO: 28/05/2020 dusanjencik: We should check also correct version
//                condition -> {
//                    moveToNextState(PrerequisitesError.InvalidVersionOfGooglePlayServices(dependencyHolder))
//                }
                    else -> {
                        moveToNextState(RegisterToFramework(dependencyHolder, true))
                    }
                }
            }
        }
    }

    /**
     * If prerequisites not met, this is class for explaining the errors.
     */
    sealed class PrerequisitesError : ExposureNotificationPhase() {

        private lateinit var moveToNextState: (ExposureNotificationPhase) -> Unit

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
         * Google play services are not available on the phone.
         */
        data class UnavailableGooglePlayServices(
            override val dependencyHolder: DependencyHolder
        ) : PrerequisitesError()

        /**
         * The current Google play services version is not matching Exposure notification minimum version.
         */
        data class InvalidVersionOfGooglePlayServices(
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
                when {
                    register -> {
                        exposureNotificationRepository.registerAppForExposureNotifications()
                    }
                    else -> {
                        exposureNotificationRepository.unregisterAppFromExposureNotifications()
                    }
                }
            }
            moveToNextState(CheckingFrameworkError(dependencyHolder, register))
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
                disposables += dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                    .subscribe { wantedState ->
                        if (wantedState.not()) {
                            moveToNextState(WaitingForWantedState(dependencyHolder))
                        }
                    }
                disposables += exposureNotificationRepository.observeRegistrationState()
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
                                if (state.error is ApiException) {
                                    // TODO: 04/06/2020 dusanjencik: check 10: at.roteskreuz.stopcorona.stage not enabled
                                    moveToNextState(
                                        RegisterActionUserApprovalNeeded(dependencyHolder, state.error as ApiException, register)
                                    )
                                } else {
                                    moveToNextState(FrameworkError.Unknown(dependencyHolder, state.error, register))
                                }
                            }
                        }
                    }
            }
        }
    }

    /**
     * User must confirm the registration app to the framework.
     * @param register True to start it, false to stop it.
     */
    data class RegisterActionUserApprovalNeeded(
        override val dependencyHolder: DependencyHolder,
        val apiException: ApiException,
        val register: Boolean
    ) : ExposureNotificationPhase() {

        private lateinit var moveToNextState: (ExposureNotificationPhase) -> Unit

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            this.moveToNextState = moveToNextState
            disposables += dependencyHolder.dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                .subscribe { wantedState ->
                    if (wantedState.not()) {
                        moveToNextState(RegisterToFramework(dependencyHolder, false))
                    }
                }
        }

        fun onResolutionOk() {
            dependencyHolder.exposureNotificationRepository.onExposureNotificationRegistrationResolutionResultOk()
            moveToNextState(CheckingFrameworkError(dependencyHolder, register))
        }

        fun onResolutionNotOk() {
            dependencyHolder.exposureNotificationRepository.onExposureNotificationRegistrationResolutionResultNotOk()
            moveToNextState(FrameworkError.RegistrationNotApproved(dependencyHolder, register))
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
                .subscribe { wantedState ->
                    if (wantedState.not()) {
                        moveToNextState(RegisterToFramework(dependencyHolder, false))
                    }
                }
        }

        /**
         * Will refresh exposure notification registration state again.
         */
        fun refresh() {
            moveToNextState(RegisterToFramework(dependencyHolder, register))
        }

        /**
         * Registration to the framework has not been approved by user.
         */
        data class RegistrationNotApproved(
            override val dependencyHolder: DependencyHolder,
            override val register: Boolean
        ) : FrameworkError()

        /**
         * Registration to the framework failed.
         */
        data class FrameworkStartFailed(
            override val dependencyHolder: DependencyHolder,
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
    }

    // TODO: 03/06/2020 dusanjencik: Check bluetooth

    /**
     * Checking if framework is registered and running.
     */
    data class CheckingFrameworkRunning(
        override val dependencyHolder: DependencyHolder
    ) : ExposureNotificationPhase() {

        override fun onCreate(moveToNextState: (ExposureNotificationPhase) -> Unit) {
            with(dependencyHolder) {
                disposables += dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
                    .subscribe { wantedState ->
                        if (wantedState.not()) {
                            moveToNextState(RegisterToFramework(dependencyHolder, false))
                        }
                    }
                disposables += exposureNotificationRepository.observeRegistrationState()
                    .filter { it is State.Idle }
                    .switchMap { exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications() }
                    .subscribe { realState ->
                        if (realState) {
                            moveToNextState(FrameworkRunning(dependencyHolder))
                        } else {
                            moveToNextState(FrameworkError.FrameworkStartFailed(dependencyHolder, true))
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
                disposables += Observables.combineLatest(
                    dashboardRepository.observeUserWantsToRegisterAppForExposureNotification(),
                    exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
                ).subscribe { (wantedState, realState) ->
                    if (wantedState.not() || realState.not()) {
                        moveToNextState(RegisterToFramework(dependencyHolder, false))
                    }
                }
            }
        }
    }
}