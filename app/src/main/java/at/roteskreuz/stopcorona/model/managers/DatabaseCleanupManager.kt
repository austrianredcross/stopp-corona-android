package at.roteskreuz.stopcorona.model.managers

import at.roteskreuz.stopcorona.model.db.dao.InfectionMessageDao
import at.roteskreuz.stopcorona.model.db.dao.TemporaryExposureKeysDao
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.ConfigurationRepository
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.AppDispatchers
import at.roteskreuz.stopcorona.utils.startOfTheDay
import at.roteskreuz.stopcorona.utils.toRollingStartIntervalNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
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

    /**
     * Removes all outgoing yellow temporary exposure keys when the user revokes probably sick status.
     */
    suspend fun removeSentYellowTemporaryExposureKeys()
}

class DatabaseCleanupManagerImpl(
    private val appDispatchers: AppDispatchers,
    private val configurationRepository: ConfigurationRepository,
    private val infectionMessageDao: InfectionMessageDao,
    private val temporaryExposureKeysDao: TemporaryExposureKeysDao
) : DatabaseCleanupManager, CoroutineScope, KoinComponent {

    companion object {
        private const val THRESHOLD_REMOVE_INCOMING_GREEN_MESSAGES = 3L //in days
        private val UNIX_TIME_START: ZonedDateTime =
            ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())
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

    override suspend fun removeSentYellowTemporaryExposureKeys() {
        temporaryExposureKeysDao.removeSentInfectionMessagesOlderThan(
            MessageType.InfectionLevel.Yellow,
            ZonedDateTime.now().toRollingStartIntervalNumber()
        )
    }

    private fun cleanupInfectionMessages() {
        cleanupReceivedInfectionMessages()
        cleanupSentTemporaryExposureKeys()
    }

    private fun cleanupReceivedInfectionMessages() {
        launch {
            val configuration = configurationRepository.observeConfiguration().blockingFirst()

            val redWarningQuarantine = configuration.redWarningQuarantine?.toLong()
            val thresholdRedMessages = if (redWarningQuarantine != null) {
                ZonedDateTime.now().minusHours(redWarningQuarantine)
            } else {
                UNIX_TIME_START
            }
            infectionMessageDao.removeReceivedInfectionMessagesOlderThan(
                MessageType.InfectionLevel.Red,
                thresholdRedMessages
            )

            val yellowWarningQuarantine = configuration.yellowWarningQuarantine?.toLong()
            val thresholdYellowMessages = if (yellowWarningQuarantine != null) {
                ZonedDateTime.now().minusHours(yellowWarningQuarantine)
            } else {
                UNIX_TIME_START
            }
            infectionMessageDao.removeReceivedInfectionMessagesOlderThan(
                MessageType.InfectionLevel.Yellow,
                thresholdYellowMessages
            )

            val thresholdGreenMessages =
                ZonedDateTime.now().minusDays(THRESHOLD_REMOVE_INCOMING_GREEN_MESSAGES)
            infectionMessageDao.removeReceivedInfectionMessagesOlderThan(
                MessageType.Revoke.Suspicion,
                thresholdGreenMessages
            )
        }
    }

    private fun cleanupSentTemporaryExposureKeys() {
        launch {
            val configuration = configurationRepository.getConfiguration()

            val nowAsRollingStartIntervalNumber = ZonedDateTime.now()
                .toRollingStartIntervalNumber()
            temporaryExposureKeysDao.removeSentInfectionMessagesOlderThan(
                MessageType.Revoke.Suspicion,
                nowAsRollingStartIntervalNumber
            )

            val yellowWarningQuarantine = configuration.yellowWarningQuarantine?.toLong()
            val thresholdYellowMessageAsRollingStart = if (yellowWarningQuarantine != null) {
                ZonedDateTime.now()
                    .startOfTheDay()
                    // +1 hour buffer time to avoid removing from midnight, since at this moment (12-Jun-2020)
                    // the rolling start interval is set by the framework at midnight (start of the day)
                    .minusHours(yellowWarningQuarantine + 1)
                    .toRollingStartIntervalNumber()
            } else {
                UNIX_TIME_START.toRollingStartIntervalNumber()
            }
            temporaryExposureKeysDao.removeSentInfectionMessagesOlderThan(
                MessageType.InfectionLevel.Yellow,
                thresholdYellowMessageAsRollingStart
            )

            val redWarningQuarantine = configuration.redWarningQuarantine?.toLong()
            val thresholdRedMessagesAsRollingStart = if (redWarningQuarantine != null) {
                ZonedDateTime.now()
                    .startOfTheDay()
                    // +1 hour buffer time to avoid removing from midnight, since at this moment (12-Jun-2020)
                    // the rolling start interval is set by the framework at midnight (start of the day)
                    .minusHours(redWarningQuarantine + 1)
                    .toRollingStartIntervalNumber()
            } else {
                UNIX_TIME_START.toRollingStartIntervalNumber()
            }
            temporaryExposureKeysDao.removeSentInfectionMessagesOlderThan(
                MessageType.InfectionLevel.Red,
                thresholdRedMessagesAsRollingStart
            )
        }
    }
}
