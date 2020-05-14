package at.roteskreuz.stopcorona.screens.dashboard

import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.manager.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.repositories.*
import at.roteskreuz.stopcorona.model.repositories.other.ContextInteractor
import at.roteskreuz.stopcorona.model.services.startCoronaDetectionService
import at.roteskreuz.stopcorona.model.services.stopCoronaDetectionService
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime

/**
 * Handles the user interaction and provides data for [DashboardFragment].
 */
class DashboardViewModel(
    appDispatchers: AppDispatchers,
    private val dashboardRepository: DashboardRepository,
    private val infectionMessengerRepository: InfectionMessengerRepository,
    private val quarantineRepository: QuarantineRepository,
    private val configurationRepository: ConfigurationRepository,
    private val contextInteractor: ContextInteractor,
    private val coronaDetectionRepository: CoronaDetectionRepository,
    private val databaseCleanupManager: DatabaseCleanupManager
) : ScopedViewModel(appDispatchers) {

    companion object {
        const val DEFAULT_RED_WARNING_QUARANTINE = 336 // hours
        const val DEFAULT_YELLOW_WARNING_QUARANTINE = 168 // hours
    }

    val showMicrophoneExplanationDialog: Boolean
        get() = dashboardRepository.showMicrophoneExplanationDialog

    var wasServiceEnabledAutomaticallyOnFirstStart: Boolean
        get() = coronaDetectionRepository.serviceEnabledOnFirstStart
        set(value) {
            coronaDetectionRepository.serviceEnabledOnFirstStart = value
        }

    var batteryOptimizationDialogShown = false

    fun observeSavedEncounters(): Observable<Int> {
        return dashboardRepository.observeSavedEncountersNumber()
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

    fun onAutomaticHandshakeEnabled(enabled: Boolean) {
        when {
            enabled && coronaDetectionRepository.isServiceRunning.not() -> {
                contextInteractor.applicationContext.startCoronaDetectionService()
            }
            enabled.not() && coronaDetectionRepository.isServiceRunning -> {
                contextInteractor.applicationContext.stopCoronaDetectionService()
            }
        }
    }

    fun observeAutomaticHandshake() = coronaDetectionRepository.observeIsServiceRunning()
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

