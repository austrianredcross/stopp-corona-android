package at.roteskreuz.stopcorona.model.managers

import at.roteskreuz.stopcorona.model.db.dao.InfectionMessageDao
import at.roteskreuz.stopcorona.model.db.dao.TemporaryExposureKeysDao
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.ConfigurationRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineRepository
import at.roteskreuz.stopcorona.model.repositories.QuarantineStatus
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.threeten.bp.ZonedDateTime
import kotlin.coroutines.CoroutineContext

/**
 * Manages the cleanup of old contact, received messages entries and sent temporary exposure keys
 * in the database.
 */
interface DatabaseCleanupManager {

    /**
     * Remove incoming green messages immediately.
     */
    suspend fun removeReceivedGreenMessages()

    // TODO: To be used when revoking a diagnostic. The former method `removeSentYellowMessages`
    //  was used when revoking a yellow state - see method `uploadRevokeSuspicionInfo()` in ReportingRepository
    //  in branch release_1.2
    /**
     * Removes all sent temporary exposure keys when the user revokes probably sick status.
     */
    suspend fun removeSentTemporaryExposureKeys()
}

class DatabaseCleanupManagerImpl(
    private val appDispatchers: AppDispatchers,
    private val configurationRepository: ConfigurationRepository,
    private val infectionMessageDao: InfectionMessageDao,
    private val temporaryExposureKeysDao: TemporaryExposureKeysDao,
    private val quarantineRepository: QuarantineRepository
) : DatabaseCleanupManager, CoroutineScope, KoinComponent {

    companion object {
        const val THRESHOLD_REMOVE_INCOMING_GREEN_MESSAGES = 3L //in days
    }

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    init {
        cleanupInfectionMessages()
    }

    override suspend fun removeReceivedGreenMessages() {
        infectionMessageDao.removeReceivedInfectionMessagesOlderThan(
            MessageType.Revoke.Suspicion,
            ZonedDateTime.now()
        )
    }

    override suspend fun removeSentTemporaryExposureKeys() {
        temporaryExposureKeysDao.removeSentTemporaryExposureKeys()
    }

    private fun cleanupInfectionMessages() {
        cleanupReceivedInfectionMessages()
        cleanupSentTemporaryExposureKeys()
    }

    private fun cleanupReceivedInfectionMessages() {
        launch {
            val configuration = configurationRepository.observeConfiguration().blockingFirst()

            val thresholdRedMessages = ZonedDateTime.now().minusHours(configuration.redWarningQuarantine?.toLong() ?: Long.MAX_VALUE)
            infectionMessageDao.removeReceivedInfectionMessagesOlderThan(MessageType.InfectionLevel.Red, thresholdRedMessages)

            val thresholdYellowMessages = ZonedDateTime.now().minusHours(configuration.yellowWarningQuarantine?.toLong() ?: Long.MAX_VALUE)
            infectionMessageDao.removeReceivedInfectionMessagesOlderThan(MessageType.InfectionLevel.Yellow, thresholdYellowMessages)

            val thresholdGreenMessages = ZonedDateTime.now().minusDays(THRESHOLD_REMOVE_INCOMING_GREEN_MESSAGES)
            infectionMessageDao.removeReceivedInfectionMessagesOlderThan(MessageType.Revoke.Suspicion, thresholdGreenMessages)
        }
    }

    private fun cleanupSentTemporaryExposureKeys() {
        launch {
            // The sent temporary exposure keys are removed in case we detect that we are not anymore in quarantine.
            if (quarantineRepository.getQuarantineStatus() is QuarantineStatus.Free) {
                temporaryExposureKeysDao.removeSentTemporaryExposureKeys()
            }
        }
    }
}
