package at.roteskreuz.stopcorona.model.entities.session

import androidx.room.*
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.skeleton.core.model.db.converters.EnumTypeConverter
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import org.threeten.bp.ZonedDateTime

/**
 * Keep state of an exposure matching session.
 */
@Entity(
    tableName = "session"
)
data class DbSession(
    @PrimaryKey()
    var currentToken: String = "",
    val warningType: WarningType,
    var processingPhase: ProcessingPhase,
    var yellowDay: ZonedDateTime?
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
    var session: DbSession,

    @Relation(
        entity = DbFullBatchPart::class,
        parentColumn = "currentToken",
        entityColumn = "currentToken"
    )
    val fullBatchParts: List<DbFullBatchPart>,

    @Relation(
        entity = DbDailyBatchPart::class,
        parentColumn = "currentToken",
        entityColumn = "currentToken"
    )
    var dailyBatchesParts: List<DbDailyBatchPart>
)

interface DbBatchPart {
    val currentToken: String
    val batchNumber: Int
    val intervalStart: Long
    val fileName: String
}

@Entity(
    tableName = "full_batch",
    indices = [
        Index("currentToken")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbSession::class,
            parentColumns = ["currentToken"],
            childColumns = ["currentToken"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DbFullBatchPart(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    override val currentToken: String = "", // will be set up during inserting to DB
    override val batchNumber: Int,
    override val intervalStart: Long,
    override val fileName: String
) : DbEntity, DbBatchPart

@Entity(
    tableName = "daily_batch",
    indices = [
        Index("currentToken")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbSession::class,
            parentColumns = ["currentToken"],
            childColumns = ["currentToken"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)

data class DbDailyBatchPart(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    override val currentToken: String = "", // will be set up during inserting to DB
    override val batchNumber: Int,
    override val intervalStart: Long,
    override val fileName: String
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
