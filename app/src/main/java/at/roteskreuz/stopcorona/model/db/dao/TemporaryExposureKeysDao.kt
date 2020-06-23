package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.exposure.DbSentTemporaryExposureKeys
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.TekMetadata
import io.reactivex.Flowable

/**
 * DAO to manage [DbSentTemporaryExposureKeys].
 */
@Dao
abstract class TemporaryExposureKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateTemporaryExposureKey(record: DbSentTemporaryExposureKeys)

    @Query("SELECT * FROM sent_temporary_exposure_keys")
    abstract fun observeSentTemporaryExposureKeys(): Flowable<List<DbSentTemporaryExposureKeys>>

    @Query("SELECT * FROM sent_temporary_exposure_keys WHERE messageType = :messageType")
    abstract suspend fun getSentTemporaryExposureKeysByMessageType(messageType: MessageType): List<DbSentTemporaryExposureKeys>

    @Query("SELECT * FROM sent_temporary_exposure_keys")
    abstract suspend fun getSentTemporaryExposureKeys(): List<DbSentTemporaryExposureKeys>

    @Transaction
    open suspend fun insertSentTemporaryExposureKeys(
        exposureKeys: List<TekMetadata>
    ) {
        exposureKeys.forEach { exposureKeyWrapper ->
            insertOrUpdateTemporaryExposureKey(
                DbSentTemporaryExposureKeys(
                    exposureKeyWrapper.rollingStartIntervalNumber,
                    exposureKeyWrapper.password,
                    exposureKeyWrapper.messageType
                )
            )
        }
    }

    @Query("DELETE FROM sent_temporary_exposure_keys WHERE messageType = :messageType AND rollingStartIntervalNumber < :olderThan")
    abstract suspend fun removeSentInfectionMessagesOlderThan(
        messageType: MessageType,
        olderThan: Int
    )
}