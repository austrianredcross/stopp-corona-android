package at.roteskreuz.stopcorona.screens.base

import at.roteskreuz.stopcorona.model.db.dao.InfectionMessageDao
import at.roteskreuz.stopcorona.model.entities.infection.message.InfectionMessageContent
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.NotificationsRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.skeleton.core.screens.base.viewmodel.ScopedViewModel
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime

/**
 * Special viewModel for managing debug tasks.
 *
 * The content in this class might not have fulfill our code quality standards. It's just for debugging.
 */
class DebugViewModel(
    appDispatchers: AppDispatchers,
    private val notificationsRepository: NotificationsRepository,
    private val quarantineRepository: QuarantineRepository,
    private val infectionMessageDao: InfectionMessageDao
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

    fun displayEndQuarantineNotification() {
        launch {
            quarantineRepository.setShowQuarantineEnd()
            notificationsRepository.displayEndQuarantineNotification()
        }
    }

    fun getQuarantineStatus(): QuarantineStatus {
        return quarantineRepository.observeQuarantineState().blockingFirst()
    }

    fun addIncomingMessageRed() {
        launch {
            val infectionMessageContent = InfectionMessageContent(MessageType.InfectionLevel.Red, ZonedDateTime.now())
            val dbMessage = infectionMessageContent.asReceivedDbEntity()
            infectionMessageDao.insertOrUpdateInfectionMessage(dbMessage)
        }
    }

    fun addIncomingMessageYellow() {
        launch {
            val infectionMessageContent = InfectionMessageContent(MessageType.InfectionLevel.Yellow, ZonedDateTime.now())
            val dbMessage = infectionMessageContent.asReceivedDbEntity()
            infectionMessageDao.insertOrUpdateInfectionMessage(dbMessage)
        }
    }
}