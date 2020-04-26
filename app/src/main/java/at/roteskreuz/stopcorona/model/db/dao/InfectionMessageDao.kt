package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.infection.message.*
import io.reactivex.Flowable
import org.threeten.bp.ZonedDateTime

/**
 * DAO to manage [DbInfectionMessage].
 */
@Dao
abstract class InfectionMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInfectionMessage(record: DbInfectionMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInfectionMessages(records: List<DbInfectionMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertOrUpdateContactWithInfectionMessage(contacts: DbContactWithInfectionMessage)

    @Transaction
    open suspend fun insertSentInfectionMessages(
        infectionMessagesWithContactEvents: List<Pair<ByteArray, InfectionMessageContent>>
    ) {
        infectionMessagesWithContactEvents.forEach { (publicKey, infectionMessage) ->
            insertOrUpdateInfectionMessage(infectionMessage.asDbEntity())
            insertOrUpdateContactWithInfectionMessage(DbContactWithInfectionMessage(infectionMessage.uuid, publicKey))
        }
    }

    @Query("SELECT * FROM infection_message WHERE isReceived = 1")
    abstract fun observeReceivedInfectionMessages(): Flowable<List<DbInfectionMessage>>

    @Query("SELECT * FROM infection_message WHERE isReceived = 0")
    abstract fun observeSentInfectionMessages(): Flowable<List<DbInfectionMessage>>

    @Transaction
    @Query("SELECT * FROM infection_message WHERE isReceived = 0 AND messageType = :messageType")
    abstract fun observeSentInfectionMessagesByMessageType(messageType: MessageType): Flowable<List<DbInfectionFullContainer>>

    @Query("DELETE FROM infection_message WHERE isReceived = :isReceived AND messageType = :messageType AND timeStamp < :olderThan")
    abstract suspend fun removeInfectionMessagesOlderThan(isReceived: Boolean, messageType: MessageType, olderThan: ZonedDateTime)
}
