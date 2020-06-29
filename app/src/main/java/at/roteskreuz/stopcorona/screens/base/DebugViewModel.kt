package at.roteskreuz.stopcorona.screens.base

import android.content.SharedPreferences
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.*
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.minusDays
import kotlinx.coroutines.launch
import org.threeten.bp.Instant

/**
 * Special viewModel for managing debug tasks.
 *
 * The content in this class might not have fulfill our code quality standards. It's just for debugging.
 */
class DebugViewModel(
    appDispatchers: AppDispatchers,
    private val diagnosisKeysRepository: DiagnosisKeysRepository,
    private val notificationsRepository: NotificationsRepository,
    private val quarantineRepository: QuarantineRepository,
    private val configurationRepository: ConfigurationRepository
) : ScopedViewModel(appDispatchers) {


    fun displayInfectionNotification(infectionLevel: MessageType.InfectionLevel) {
        launch {
            notificationsRepository.displayInfectionNotification(infectionLevel)
        }
    }

    fun displaySelfRetestNotification() {
        launch {
            notificationsRepository.displaySelfRetestNotification()
        }
    }

    fun displaySomeoneHasRecoveredNotification() {
        launch {
            diagnosisKeysRepository.setSomeoneHasRecovered()
            notificationsRepository.displaySomeoneHasRecoveredNotification()
        }
    }

    fun displayEndQuarantineNotification() {
        launch {
            quarantineRepository.setShowQuarantineEnd()
            notificationsRepository.displayEndQuarantineNotification()
        }
    }

    fun getQuarantineStatus(): QuarantineStatus {
        return quarantineRepository.observeQuarantineState().blockingFirst()
    }

    fun reportMedicalConfirmation() {
        launch {
            // Only set the sickness report date. Does not store keys in the saved-TEK data base
            quarantineRepository.reportMedicalConfirmation()
        }
    }

    fun reportPositiveSelfDiagnose() {
        launch {
            // Only set the sickness report date. Does not store keys in the saved-TEK data base
            quarantineRepository.reportPositiveSelfDiagnose()
        }
    }

    fun quarantineRedForZeroDays() {
        launch(appDispatchers.IO) {
            val redQuarantineHours = configurationRepository.getConfiguration()!!.redWarningQuarantine!!
            val quarantineDay = Instant.now().minusDays((redQuarantineHours / 24).toLong())
            quarantineRepository.receivedWarning(WarningType.RED, quarantineDay)
        }
    }

    fun quarantineYellowForZeroDays() {
        launch(appDispatchers.IO) {
            val yellowQuaratineHours = configurationRepository.getConfiguration()!!.yellowWarningQuarantine!!
            val quarantineDay = Instant.now().minusDays((yellowQuaratineHours / 24).toLong())
            quarantineRepository.receivedWarning(WarningType.YELLOW, quarantineDay)
        }
    }

    fun displayNotificationForUploadingKeysFromTheDayBefore() {
        notificationsRepository.displayNotificationForUploadingKeysFromTheDayBefore()
    }

    fun fakeReportWithMissingKeysYesterday() {
        quarantineRepository.fakeReportWithMissingKeysYesterday();
    }
}