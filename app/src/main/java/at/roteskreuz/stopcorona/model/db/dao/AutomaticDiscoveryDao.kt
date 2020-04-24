package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import at.roteskreuz.stopcorona.model.entities.discovery.DbAutomaticDiscoveryEvent
import io.reactivex.Flowable
import org.threeten.bp.ZonedDateTime

/**
 * DAO for [DbAutomaticDiscoveryEvent].
 */
@Dao
abstract class AutomaticDiscoveryDao {

    @Insert
    protected abstract suspend fun insert(event: DbAutomaticDiscoveryEvent)

    @Query("UPDATE automatic_discovery SET endTime = :endTime WHERE publicKey = :publicKey AND endTime is NULL")
    protected abstract suspend fun fillEndDate(publicKey: ByteArray, endTime: ZonedDateTime)

    @Query("SELECT publicKey FROM automatic_discovery GROUP BY publicKey")
    protected abstract suspend fun allPublicKeys(): List<ByteArray>

    @Query("DELETE FROM automatic_discovery")
    abstract suspend fun deleteAllEvents()

    /**
     * Delete all events which ended before the [endTime]. And 0 proximity events if they started
     * before the [endTime].
     *
     * Zero proximity events (PeerLost) at the beginning of the event list are not relevant and might otherwise
     * never be deleted if we never see the device again.
     */
    @Query("DELETE FROM automatic_discovery WHERE (endTime < :endTime) OR (startTime < :endTime AND proximity = 0)")
    abstract suspend fun deleteOldEvents(endTime: ZonedDateTime)

    @Query("SELECT * FROM automatic_discovery ORDER BY publicKey")
    abstract fun observeAllEvents(): Flowable<List<DbAutomaticDiscoveryEvent>>

    @Transaction
    open suspend fun insertEventAndTerminatePrevious(publicKey: ByteArray, proximity: Int, startTime: ZonedDateTime = ZonedDateTime.now()) {
        fillEndDate(publicKey, startTime)
        insert(DbAutomaticDiscoveryEvent(publicKey = publicKey, proximity = proximity, startTime = startTime))
    }

    @Transaction
    open suspend fun insertEventForAllPeersAndTerminatePrevious(proximity: Int, start: ZonedDateTime = ZonedDateTime.now()) {
        allPublicKeys().forEach { publicKey ->
            insertEventAndTerminatePrevious(publicKey, proximity, start)
        }
    }

}