package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.infection.message.DbReceivedInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.DbSentInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.InfectionMessageContent
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import io.reactivex.Flowable
import org.threeten.bp.ZonedDateTime

/**
 * DAO to manage [DbSentInfectionMessage], [DbReceivedInfectionMessage].
 */
@Dao
abstract class InfectionMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInfectionMessage(record: DbReceivedInfectionMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInfectionMessage(record: DbSentInfectionMessage)

    @Transaction
    open suspend fun insertSentInfectionMessages(
        infectionMessagesWithContactEvents: List<Pair<ByteArray, InfectionMessageContent>>
    ) {
        infectionMessagesWithContactEvents.forEach { (publicKey, infectionMessage) ->
            insertOrUpdateInfectionMessage(infectionMessage.asSentDbEntity(publicKey))
        }
    }

    @Query("SELECT * FROM received_infection_message")
    abstract fun observeReceivedInfectionMessages(): Flowable<List<DbReceivedInfectionMessage>>

    @Query("SELECT * FROM sent_infection_message")
    abstract fun observeSentInfectionMessages(): Flowable<List<DbSentInfectionMessage>>

    @Query("SELECT * FROM sent_infection_message WHERE messageType = :messageType")
    abstract suspend fun getSentInfectionMessagesByMessageType(messageType: MessageType): List<DbSentInfectionMessage>

    @Query("DELETE FROM sent_infection_message WHERE messageType = :messageType AND timeStamp < :olderThan")
    abstract suspend fun removeSentInfectionMessagesOlderThan(messageType: MessageType, olderThan: ZonedDateTime)

    @Query("DELETE FROM received_infection_message WHERE messageType = :messageType AND timeStamp < :olderThan")
    abstract suspend fun removeReceivedInfectionMessagesOlderThan(messageType: MessageType, olderThan: ZonedDateTime)

    @Query("SELECT COUNT(*) > 0  FROM received_infection_message WHERE messageType = 'r'")
    abstract suspend fun hasReceivedRedInfectionMessages(): Boolean
}
