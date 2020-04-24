package at.roteskreuz.stopcorona.screens.base

import at.roteskreuz.stopcorona.model.db.dao.InfectionMessageDao
import at.roteskreuz.stopcorona.model.db.dao.NearbyRecordDao
import at.roteskreuz.stopcorona.model.entities.infection.message.InfectionMessageContent
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.InfectionMessengerRepository
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import at.roteskreuz.stopcorona.utils.view.safeRun
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import kotlin.random.Random

/**
 * Special viewModel for managing debug tasks.
 *
 * The content in this class might not have fulfill our code quality standards. It's just for debugging.
 */
class DebugViewModel(
    appDispatchers: AppDispatchers,
    private val infectionMessengerRepository: InfectionMessengerRepository,
    private val notificationsRepository: NotificationsRepository,
    private val quarantineRepository: QuarantineRepository,
    private val infectionMessageDao: InfectionMessageDao,
    private val nearbyRecordDao: NearbyRecordDao
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
            infectionMessengerRepository.setSomeoneHasRecovered()
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

    fun addOutgoingMessageRed() {
        launch {
            val infectionMessageContent = InfectionMessageContent(MessageType.InfectionLevel.Red, ZonedDateTime.now())
            infectionMessengerRepository.storeSentInfectionMessages(listOf(ByteArray(1) to infectionMessageContent))
            quarantineRepository.reportMedicalConfirmation()
        }
    }

    fun addOutgoingMessageYellow() {
        launch {
            val infectionMessageContent = InfectionMessageContent(MessageType.InfectionLevel.Yellow, ZonedDateTime.now())
            infectionMessengerRepository.storeSentInfectionMessages(listOf(ByteArray(1) to infectionMessageContent))
            quarantineRepository.reportPositiveSelfDiagnose()
        }
    }

    fun addIncomingMessageRed() {
        launch {
            val infectionMessageContent = InfectionMessageContent(MessageType.InfectionLevel.Red, ZonedDateTime.now())
            val dbMessage = infectionMessageContent.asDbEntity().copy(isReceived = true)
            infectionMessageDao.insertOrUpdateInfectionMessage(dbMessage)
        }
    }

    fun addIncomingMessageYellow() {
        launch {
            val infectionMessageContent = InfectionMessageContent(MessageType.InfectionLevel.Yellow, ZonedDateTime.now())
            val dbMessage = infectionMessageContent.asDbEntity().copy(isReceived = true)
            infectionMessageDao.insertOrUpdateInfectionMessage(dbMessage)
        }
    }

    fun addIncomingMessageGreen() {
        launch {
            infectionMessageDao.observeReceivedInfectionMessages().blockingFirst()
                .firstOrNull { infectionMessage -> infectionMessage.messageType == MessageType.InfectionLevel.Yellow }
                .safeRun("Yellow message not available!") { yellowMessage ->
                    infectionMessageDao.insertOrUpdateInfectionMessage(yellowMessage.copy(messageType = MessageType.Revoke))
                    infectionMessengerRepository.setSomeoneHasRecovered()
                }
        }
    }

    fun addRandomContact() {
        launch {
            val randomValue = Random.nextInt()
            nearbyRecordDao.insert(randomValue.toString().toByteArray(), randomValue % 2 == 0)
        }
    }
}

/**
 * Code marked by this annotation should not be used in release builds.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class DebugOnly