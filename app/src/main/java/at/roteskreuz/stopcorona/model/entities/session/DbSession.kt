package at.roteskreuz.stopcorona.model.entities.session

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.skeleton.core.model.db.converters.EnumTypeConverter
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

/**
 * Keep state of an exposure matching session.
 */
@Entity(
    tableName = "session",
    indices = [
        Index("currentToken", unique = true)
    ]
)
data class DbSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val currentToken: String,
    val warningType: WarningType,
    val processingPhase: ProcessingPhase,
    val firstYellowDay: Instant?,
    val created: ZonedDateTime = ZonedDateTime.now()
) : DbEntity

enum class ProcessingPhase {
    FullBatch,
    DailyBatch
}

class ProcessingPhaseConverter : EnumTypeConverter<ProcessingPhase>({ enumValueOf(it) })

/**
 * This entity is wrapping the [DbSession], its [DbFullBatchPart] and [DbDailyBatchPart].
 */
data class DbFullSession(
    @Embedded
    val session: DbSession,

    @Relation(
        entity = DbFullBatchPart::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val fullBatchParts: List<DbFullBatchPart>,

    @Relation(
        entity = DbDailyBatchPart::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val dailyBatchesParts: List<DbDailyBatchPart>
)

val DbFullSession.remainingDailyBatchesParts
    get() = dailyBatchesParts.filter { !it.processed }

interface DbBatchPart {
    val sessionId: Long
    val batchNumber: Int
    val intervalStart: Long
    val fileName: String
}

@Entity(
    tableName = "full_batch",
    indices = [
        Index("sessionId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DbFullBatchPart(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    override val sessionId: Long = 0, // will be set up during inserting to DB
    override val batchNumber: Int,
    override val intervalStart: Long,
    override val fileName: String
) : DbEntity, DbBatchPart

@Entity(
    tableName = "daily_batch",
    indices = [
        Index("sessionId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)

data class DbDailyBatchPart(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    override val sessionId: Long = 0, // will be set up during inserting to DB
    override val batchNumber: Int,
    override val intervalStart: Long,
    override val fileName: String,
    val processed: Boolean = false
) : DbEntity, DbBatchPart {

    /**
     * From Android-Exposure-Notification-API-documentation-v1.3.2 Page 16:
     * A number describing when a key starts.
     * It is equal to startTimeOfKeySinceEpochInSecs / (60 * 10).
     * */
    fun intervalStartAsEpochSeconds(): Long {
        return intervalStart * 600
    }
}
