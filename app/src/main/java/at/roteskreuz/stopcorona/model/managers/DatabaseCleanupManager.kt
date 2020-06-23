package at.roteskreuz.stopcorona.model.managers

import at.roteskreuz.stopcorona.model.db.dao.TemporaryExposureKeysDao
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.exceptions.SilentError
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
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Manages the cleanup of old contact, received messages entries and sent temporary exposure keys
 * in the database.
 */
interface DatabaseCleanupManager

class DatabaseCleanupManagerImpl(
    private val appDispatchers: AppDispatchers,
    private val configurationRepository: ConfigurationRepository,
    private val temporaryExposureKeysDao: TemporaryExposureKeysDao
) : DatabaseCleanupManager, CoroutineScope, KoinComponent {

    companion object {
        private val UNIX_TIME_START: ZonedDateTime =
            ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())
    }

    override val coroutineContext: CoroutineContext
        get() = appDispatchers.Default

    init {
        cleanupInfectionMessages()
    }

    private fun cleanupInfectionMessages() {
        cleanupSentTemporaryExposureKeys()
    }

    // TODO: Adjust the clean-up knowing that we might need to re-use old keys.
    private fun cleanupSentTemporaryExposureKeys() {
        launch {
            val configuration = configurationRepository.getConfiguration() ?: run {
                Timber.e(SilentError(IllegalStateException("no configuration present, failing silently")))
                return@launch
            }

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
