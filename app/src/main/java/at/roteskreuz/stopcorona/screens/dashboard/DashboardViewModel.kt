package at.roteskreuz.stopcorona.screens.dashboard

import android.app.Activity
import android.content.Context
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.manager.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.repositories.*
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.NonNullableBehaviorSubject
import com.github.dmstocking.optional.java.util.Optional
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Handles the user interaction and provides data for [DashboardFragment].
 */
class DashboardViewModel(
    appDispatchers: AppDispatchers,
    private val dashboardRepository: DashboardRepository,
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
    private val prerequisitesErrorSubject = NonNullableBehaviorSubject<Optional<CombinedExposureNotificationsState.EnabledWithError>>(
        Optional.ofNullable(null)
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
    private fun onRegisterToExposureFramework(register: Boolean) {
        //TODO: Falko refresh state in exposureNotificationRepository
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
            prerequisitesErrorSubject
//            exposureNotificationRepository.observeRegistrationState(),
//            exposureNotificationRepository.observeAppIsRegisteredForExposureNotifications()
        )
            .debounce(50, TimeUnit.MILLISECONDS) // some of the sources can be changed together
            .map { (wantedState, prerequisitesErrors) -> //, registrationState, registrationStatess ->
                when {
                    wantedState && prerequisitesErrors.isPresent -> prerequisitesErrors.get()
                    wantedState -> CombinedExposureNotificationsState.Enabled
//                wantedState && registr
//                wantedState && realState -> CombinedExposureNotificationsState.Enabled
                    else -> CombinedExposureNotificationsState.Disabled
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

    private fun refreshExposureNotificationAppRegisteredState() {
        exposureNotificationRepository.refreshExposureNotificationAppRegisteredState()
    }

    fun checkExposureNotificationPrerequisites(context: Context) {
        var error: CombinedExposureNotificationsState.EnabledWithError? = null
        if (googlePlayAvailability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            // TODO: 28/05/2020 dusanjencik: We should check also correct version
            onRegisterToExposureFramework(userWantsToRegisterAppForExposureNotifications)
        } else {
            error = CombinedExposureNotificationsState.EnabledWithError.Prerequisites.UnavailableGooglePlayServices
        }
        prerequisitesErrorSubject.onNext(Optional.ofNullable(error))
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

/**
 * Indication for switch.
 */
sealed class CombinedExposureNotificationsState {

    sealed class EnabledWithError : CombinedExposureNotificationsState() {
        sealed class Prerequisites : EnabledWithError() {
            /**
             * Google play services are not available on the phone.
             */
            object UnavailableGooglePlayServices : EnabledWithError()

            /**
             * The current Google play services version is not matching Exposure notification minimum version.
             */
            object InvalidVersionOfGooglePlayServices : EnabledWithError()
        }

        /**
         * Error from Exposure notification framework.
         */
        data class ExposureNotificationApiException(val error: ApiException) : EnabledWithError()
    }

    object Enabled : CombinedExposureNotificationsState()
    object Disabled : CombinedExposureNotificationsState()
}