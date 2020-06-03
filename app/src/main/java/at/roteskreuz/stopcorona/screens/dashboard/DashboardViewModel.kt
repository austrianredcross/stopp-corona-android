package at.roteskreuz.stopcorona.screens.dashboard

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.manager.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.repositories.*
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.github.dmstocking.optional.java.util.Optional
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit

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
     * Holder for the current error state.
     * Used to check prerequisites to register to Exposure notification framework.
     */
    private val prerequisitesErrorSubject = NonNullableBehaviorSubject<Optional<CombinedExposureNotificationsState.EnabledWithError.Prerequisites>>(
        Optional.ofNullable(null)
    )

    private val registrationErrorSubject = NonNullableBehaviorSubject<Optional<Throwable>>(
        Optional.ofNullable(null)
    )

    private val exposureCheckPhaseSubject = NonNullableBehaviorSubject<ExposureCheckPhase>(
        ExposureCheckPhase.WaitingForWantedState
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

        disposables += dashboardRepository.observeUserWantsToRegisterAppForExposureNotification()
            .subscribe { wantedState ->
                if (wantedState) {
                    exposureCheckPhaseSubject.onNext(ExposureCheckPhase.CheckPrerequisites)
                } else {
                    exposureCheckPhaseSubject.onNext(ExposureCheckPhase.WaitingForWantedState)
                }
            }

        disposables += exposureCheckPhaseSubject
            .subscribe { phase ->
                when (phase) {
                    ExposureCheckPhase.WaitingForWantedState -> {
                        // do nothing
                    }
                    ExposureCheckPhase.CheckPrerequisites -> {
                        val error = checkExposureNotificationPrerequisitesAndGetError()
                        prerequisitesErrorSubject.onNext(Optional.ofNullable(error))
                        if (error == null) {
                            exposureCheckPhaseSubject.onNext(ExposureCheckPhase.RegistrationActing)
                        }
                    }
                    ExposureCheckPhase.RegistrationActing -> {
                        registerToExposureFramework(userWantsToRegisterAppForExposureNotifications)
                    }
                    ExposureCheckPhase.Running -> {
                        // do nothing
                    }
                }
            }

        disposables += Observables.combineLatest(
            exposureCheckPhaseSubject,
            exposureNotificationRepository.observeRegistrationState()
        )
            .subscribe { (phase, state) ->
                if (phase == ExposureCheckPhase.RegistrationActing) {
                    when (state) {
                        State.Idle -> TODO()
                        is State.Error -> {
                            registrationErrorSubject.onNext(Optional.ofNullable(state.error))
                        }
                    }
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

    /**
     * @param register True to start it, false to stop it.
     */
    private fun registerToExposureFramework(register: Boolean) {
        when {
            register && exposureNotificationRepository.isAppRegisteredForExposureNotifications.not() -> {
                exposureNotificationRepository.registerAppForExposureNotifications()
            }
            register.not() && exposureNotificationRepository.isAppRegisteredForExposureNotifications -> {
                exposureNotificationRepository.unregisterAppFromExposureNotifications()
            }
        }
    }

    fun observeCombinedExposureNotificationState(): Observable<CombinedExposureNotificationsState> {
        return Observables.combineLatest(
            dashboardRepository.observeUserWantsToRegisterAppForExposureNotification(),
            prerequisitesErrorSubject,
            exposureNotificationRepository.observeRegistrationState()
                // ignoring idle and loading states
                .map { state ->
                    if (state is State.Error) {
                        Optional.of(state.error)
                    } else {
                        Optional.ofNullable(null)
                    }
                },
            exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
        ) { wantedState, prerequisitesError, registrationError, runningState ->
            CombinedExposureNotificationSet(
                wantedState,
                prerequisitesError.orElse(null),
                registrationError.orElse(null),
                runningState
            )
        }
            .debounce(50, TimeUnit.MILLISECONDS) // some of the values can be changed together
            .map { combinedExposureNotificationSet ->
                with(combinedExposureNotificationSet) {
                    when {
                        wantedState -> {
                            when {
                                prerequisitesError != null -> {
                                    prerequisitesError
                                }
                                registrationError != null -> {
                                    CombinedExposureNotificationsState.EnabledWithError.ExposureNotificationError(registrationError)
                                }
                                else -> {
                                    CombinedExposureNotificationsState.Enabled
                                }
                            }
                        }
                        else -> {
                            CombinedExposureNotificationsState.Disabled
                        }
                    }
                }
            }
            .distinctUntilChanged()
    }

    /**
     * Handles [Activity.RESULT_OK] for a resolution. User accepted opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultOk() {
        exposureNotificationRepository.onExposureNotificationRegistrationResolutionResultOk()
    }

    /**
     * Handles not [Activity.RESULT_OK] for a resolution. User rejected opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultNotOk() {
        exposureNotificationRepository.onExposureNotificationRegistrationResolutionResultNotOk()
    }

    //
//    private fun refreshExposureNotificationAppRegisteredState() {
//        exposureNotificationRepository.refreshExposureNotificationAppRegisteredState()
//    }
//
    private fun checkExposureNotificationPrerequisitesAndGetError(): CombinedExposureNotificationsState.EnabledWithError.Prerequisites? {
        var error: CombinedExposureNotificationsState.EnabledWithError.Prerequisites? = null
        if (googlePlayAvailability.isGooglePlayServicesAvailable(contextInteractor.applicationContext) != ConnectionResult.SUCCESS) {
            error = CombinedExposureNotificationsState.EnabledWithError.Prerequisites.UnavailableGooglePlayServices
        }
        // TODO: 28/05/2020 dusanjencik: We should check also correct version
        return error
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

data class CombinedExposureNotificationSet(
    val wantedState: Boolean,
    val prerequisitesError: CombinedExposureNotificationsState.EnabledWithError.Prerequisites?,
    val registrationError: Throwable?,
    val runningState: Boolean
)

/**
 * Indication for switch.
 */
sealed class CombinedExposureNotificationsState {

    sealed class EnabledWithError : CombinedExposureNotificationsState() {
        sealed class Prerequisites : EnabledWithError() {
            /**
             * Google play services are not available on the phone.
             */
            object UnavailableGooglePlayServices : Prerequisites()

            /**
             * The current Google play services version is not matching Exposure notification minimum version.
             */
            object InvalidVersionOfGooglePlayServices : Prerequisites()
        }

        /**
         * Error from Exposure notification framework.
         */
        data class ExposureNotificationError(val error: Throwable) : EnabledWithError()
    }

    object Enabled : CombinedExposureNotificationsState()
    object Disabled : CombinedExposureNotificationsState()
}

sealed class ExposureCheckPhase {
    object WaitingForWantedState : ExposureCheckPhase()
    object CheckPrerequisites : ExposureCheckPhase()
    object RegistrationActing : ExposureCheckPhase()
    object Running : ExposureCheckPhase()
}