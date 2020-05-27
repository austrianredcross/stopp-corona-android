package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.nearby.DbNearbyRecord
import io.reactivex.Flowable
import org.threeten.bp.ZonedDateTime

/**
 * DAO to manage [DbNearbyRecord].
 */
@Dao
abstract class NearbyRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(record: DbNearbyRecord): Long

    @Transaction
    open suspend fun insert(publicKey: ByteArray, detectedAutomatically: Boolean): Long {
        return insert(DbNearbyRecord(
            publicKey = publicKey,
            detectedAutomatically = detectedAutomatically
        ))
    }

    @Query("SELECT * FROM nearby_record WHERE timestamp > :time")
    abstract fun observeRecordsRecentThan(time: ZonedDateTime): Flowable<List<DbNearbyRecord>>

    @Query("SELECT COUNT() FROM nearby_record")
    abstract fun observeNumberOfRecords(): Flowable<Int>

    @Query("SELECT COUNT(*) > 0 FROM nearby_record WHERE publicKey = :publicKey AND timestamp > :timestamp")
    abstract suspend fun wasRecordSavedInGivenPeriod(publicKey: ByteArray, timestamp: ZonedDateTime): Boolean

    @Query("DELETE FROM nearby_record WHERE timestamp < :timestamp")
    abstract suspend fun removeContactOlderThan(timestamp: ZonedDateTime)
}