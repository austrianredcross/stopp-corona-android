package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.roteskreuz.stopcorona.model.entities.infection.message.DbReceivedInfectionMessage
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import io.reactivex.Flowable
import org.threeten.bp.ZonedDateTime

/**
 * DAO to manage [DbReceivedInfectionMessage].
 */
@Dao
abstract class InfectionMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInfectionMessage(record: DbReceivedInfectionMessage)

    @Query("SELECT * FROM received_infection_message")
    abstract fun observeReceivedInfectionMessages(): Flowable<List<DbReceivedInfectionMessage>>

    @Query("DELETE FROM received_infection_message WHERE messageType = :messageType AND timeStamp < :olderThan")
    abstract suspend fun removeReceivedInfectionMessagesOlderThan(messageType: MessageType, olderThan: ZonedDateTime)

    @Query("SELECT COUNT(*) > 0  FROM received_infection_message WHERE messageType = 'r'")
    abstract suspend fun hasReceivedRedInfectionMessages(): Boolean
}
