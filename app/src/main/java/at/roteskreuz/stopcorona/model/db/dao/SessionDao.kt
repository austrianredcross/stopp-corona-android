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
    protected abstract suspend fun insertFullBatchPath(batch: DbFullBatchPart): Long

    @Insert
    protected abstract suspend fun insertDailyBatchPath(batch: DbDailyBatchPart): Long

    @Transaction
    open suspend fun insertFullSession(fullSession: DbFullSession) {
        val sessionId = insertSession(fullSession.session)
        fullSession.fullBatchParts.forEach { fullBatchPathPath ->
            insertFullBatchPath(fullBatchPathPath.copy(sessionId = sessionId))
        }
        fullSession.dailyBatchesParts.forEach { dailyBatchPath ->
            insertDailyBatchPath(dailyBatchPath.copy(sessionId = sessionId))
        }
    }

    @Transaction
    @Delete
    abstract suspend fun deleteSession(session: DbSession): Int

    @Transaction
    @Query("SELECT * FROM session where currentToken = :token")
    abstract suspend fun getFullSession(token: String): DbFullSession?

    @Transaction
    @Update
    abstract suspend fun updateDailyBatchParts(dailyBatches: List<DbDailyBatchPart>)

    @Transaction
    @Update
    abstract suspend fun updateSession(session: DbSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertScheduledSession(scheduledSession: DbScheduledSession)

    @Query("SELECT COUNT(*) = 1 FROM scheduled_sessions WHERE token = :token LIMIT 1")
    abstract suspend fun isSessionScheduled(token: String): Boolean

    @Query("DELETE FROM scheduled_sessions WHERE token = :token")
    abstract suspend fun deleteScheduledSession(token: String)
}