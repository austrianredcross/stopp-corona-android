package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.exposure.DbSentTemporaryExposureKeys
import at.roteskreuz.stopcorona.model.entities.exposure.UNDEFINED_ROLLING_PERIOD
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.model.repositories.TekMetadata
import io.reactivex.Flowable

/**
 * DAO to manage [DbSentTemporaryExposureKeys].
 */
@Dao
abstract class TemporaryExposureKeysDao {

    /**
     * Delete key entries with given [DbSentTemporaryExposureKeys.rollingStartIntervalNumber] and missing [DbSentTemporaryExposureKeys.rollingPeriod]
     */
    @Query("DELETE from sent_temporary_exposure_keys WHERE rollingStartIntervalNumber in (:rollingStartIntervalNumbers) AND _rollingPeriod = -1")
    protected abstract suspend fun removeOldPartialEntries(rollingStartIntervalNumbers: Collection<Int>): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertOrUpdateTemporaryExposureKeys(record: Collection<DbSentTemporaryExposureKeys>)

    @Query("SELECT * FROM sent_temporary_exposure_keys")
    protected abstract fun observeSentTemporaryExposureKeys(): Flowable<List<DbSentTemporaryExposureKeys>>

    @Transaction
    open suspend fun insertSentTemporaryExposureKeys(
        exposureKeys: List<TekMetadata>
    ) {
        val coveredRollingStartIntervalNumbers = exposureKeys.map { exposureKeyWrapper ->
            exposureKeyWrapper.validity.rollingStartIntervalNumber
        }.distinct()
        val sentTemporaryExposureKeys = exposureKeys.map { exposureKeyWrapper ->
            DbSentTemporaryExposureKeys(
                exposureKeyWrapper.validity.rollingStartIntervalNumber,
                exposureKeyWrapper.validity.rollingPeriod ?: UNDEFINED_ROLLING_PERIOD,
                exposureKeyWrapper.password,
                exposureKeyWrapper.messageType
            )
        }
        // Remove old partial entries as full entries will be creeated during the following insert
        removeOldPartialEntries(coveredRollingStartIntervalNumbers)
        insertOrUpdateTemporaryExposureKeys(sentTemporaryExposureKeys)
    }

    @Query("SELECT * FROM sent_temporary_exposure_keys WHERE messageType = :messageType")
    abstract suspend fun getSentTemporaryExposureKeysByMessageType(messageType: MessageType): List<DbSentTemporaryExposureKeys>

    @Query("SELECT * FROM sent_temporary_exposure_keys")
    abstract suspend fun getSentTemporaryExposureKeys(): List<DbSentTemporaryExposureKeys>

    @Query("DELETE FROM sent_temporary_exposure_keys WHERE messageType = :messageType AND rollingStartIntervalNumber < :olderThan")
    abstract suspend fun removeSentTemporarelyExposureKeysOlderThan(
        messageType: MessageType,
        olderThan: Int
    ): Int

}