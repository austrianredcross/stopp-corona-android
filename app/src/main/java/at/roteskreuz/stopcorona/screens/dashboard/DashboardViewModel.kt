package at.roteskreuz.stopcorona.screens.dashboard

import android.app.Activity
import at.roteskreuz.stopcorona.model.managers.ChangelogManager
import at.roteskreuz.stopcorona.model.managers.DatabaseCleanupManager
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationManager
import at.roteskreuz.stopcorona.model.managers.ExposureNotificationPhase
import at.roteskreuz.stopcorona.model.repositories.*
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import com.github.dmstocking.optional.java.util.Optional
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import org.threeten.bp.ZonedDateTime

/**
 * Handles the user interaction and provides data for [DashboardFragment].
 */
class DashboardViewModel(
    appDispatchers: AppDispatchers,
    private val dashboardRepository: DashboardRepository,
    private val diagnosisKeysRepository: DiagnosisKeysRepository,
    private val quarantineRepository: QuarantineRepository,
    private val databaseCleanupManager: DatabaseCleanupManager,
    private val changelogManager: ChangelogManager,
    private val exposureNotificationManager: ExposureNotificationManager
) : ScopedViewModel(appDispatchers) {

    private var wasExposureFrameworkAutomaticallyEnabledOnFirstStart: Boolean
        get() = dashboardRepository.exposureFrameworkEnabledOnFirstStart
        set(value) {
            dashboardRepository.exposureFrameworkEnabledOnFirstStart = value
        }

    var userWantsToRegisterAppForExposureNotifications: Boolean
        get() = exposureNotificationManager.userWantsToRegisterAppForExposureNotifications
        set(value) {
            exposureNotificationManager.userWantsToRegisterAppForExposureNotifications = value
        }

    val currentExposureNotificationPhase: ExposureNotificationPhase
        get() = exposureNotificationManager.currentPhase

    val dateOfFirstMedicalConfirmation: ZonedDateTime?
        get() = quarantineRepository.dateOfFirstMedicalConfirmation

    init {
        /**
         * If the user starts the app for the first time the exposure notification framework will be started automatically.
         */
        if (wasExposureFrameworkAutomaticallyEnabledOnFirstStart.not()) {
            wasExposureFrameworkAutomaticallyEnabledOnFirstStart = true
            userWantsToRegisterAppForExposureNotifications = true
        }
    }

    fun observeDateOfFirstMedicalConfirmation(): Observable<Optional<ZonedDateTime>> {
        return quarantineRepository.observeDateOfFirstMedicalConfirmation()
    }

    fun observeContactsHealthStatus(): Observable<HealthStatusData> {
        return Observables.combineLatest(
            quarantineRepository.observeQuarantineState(),
            quarantineRepository.observeCombinedWarningType()
        ).map { (quarantineStatus, combinedWarningType) ->
            if (combinedWarningType.redContactsDetected || combinedWarningType.yellowContactsDetected) {
                HealthStatusData.ContactsSicknessInfo(quarantineStatus, combinedWarningType)
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

    fun observeIfUploadOfMissingExposureKeysIsNeeded(): Observable<Optional<UploadMissingExposureKeys>> {
        return quarantineRepository.observeIfUploadOfMissingExposureKeysIsNeeded()
    }

    fun observeSomeoneHasRecoveredStatus(): Observable<HealthStatusData> {
        return diagnosisKeysRepository.observeSomeoneHasRecoveredMessage()
            .map { shouldShow ->
                if (shouldShow) {
                    HealthStatusData.SomeoneHasRecovered
                } else {
                    HealthStatusData.NoHealthStatus
                }
            }
    }

    fun someoneHasRecoveredSeen() {
        diagnosisKeysRepository.someoneHasRecoveredMessageSeen()
    }

    fun observeExposureNotificationPhase(): Observable<ExposureNotificationPhase> {
        return exposureNotificationManager.observeExposureNotificationPhase()
    }

    /**
     * Handles [Activity.RESULT_OK] for a resolution. User accepted opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultOk() {
        exposureNotificationManager.onExposureNotificationRegistrationResolutionResultOk()
    }

    /**
     * Handles not [Activity.RESULT_OK] for a resolution. User rejected opt-in for exposure notification.
     */
    fun onExposureNotificationRegistrationResolutionResultNotOk() {
        exposureNotificationManager.onExposureNotificationRegistrationResolutionResultNotOk()
    }

    fun refreshPrerequisitesErrorStatement(ignoreErrors: Boolean = false) {
        exposureNotificationManager.refreshPrerequisitesErrorStatement(ignoreErrors)
    }

    fun unseenChangelogForVersionAvailable(version: String): Boolean {
        return changelogManager.unseenChangelogForVersionAvailable(version)
    }

    fun observeExposureSDKReadyToStart(): Observable<Boolean> {
        return changelogManager.observeExposureSDKReadyToStart().distinctUntilChanged()
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
        val quarantineStatus: QuarantineStatus,
        val warningType: CombinedWarningType
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