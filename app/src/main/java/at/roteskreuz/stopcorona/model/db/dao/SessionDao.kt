package at.roteskreuz.stopcorona.model.db.dao

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.session.*

/**
 * Dao to manage [DbSession], [DbScheduledSession].
 */
@Dao
abstract class SessionDao {

    @Insert
    protected abstract suspend fun insertSession(session: DbSession): Long

    @Insert
    protected abstract suspend fun insertFullBatch(batch: DbFullBatchPart): Long

    @Insert
    protected abstract suspend fun insertDailyBatch(batch: DbDailyBatchPart): Long

    @Query("DELETE FROM session WHERE token = :token")
    abstract suspend fun deleteSession(token: String): Int

    @Transaction
    open suspend fun insertOrUpdateFullSession(fullSession: DbFullSession) {
        val token = fullSession.session.token
        deleteSession(token)
        insertSession(fullSession.session)
        fullSession.fullBatchParts.forEach { fullBatchPath ->
            insertFullBatch(fullBatchPath.copy(token = token))
        }
        fullSession.dailyBatchesParts.forEach { dailyBatch ->
            insertDailyBatch(dailyBatch.copy(token = token))
        }
    }

    @Transaction
    @Query("SELECT * FROM session where token = :token")
    abstract suspend fun getFullSession(token: String): DbFullSession

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertScheduledSession(scheduledSession: DbScheduledSession)

    @Query("SELECT COUNT(*) = 1 FROM scheduled_sessions WHERE token = :token LIMIT 1")
    abstract suspend fun isSessionScheduled(token: String): Boolean

    @Query("DELETE FROM scheduled_sessions WHERE token = :token")
    abstract suspend fun deleteScheduledSession(token: String)
}