package at.roteskreuz.stopcorona.model.entities.session

import androidx.room.*
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity

/**
 * Keep state of an exposure matching session.
 */
@Entity(
    tableName = "session"
)
data class DbSession(
    @PrimaryKey()
    val token: String = "",
    val warningType: String
) : DbEntity

/**
 * This entity is wrapping hotel instance, its images and properties.
 */
data class DbFullSession(
    @Embedded
    var session: DbSession,

    @Relation(
        entity = DbFullBatchPart::class,
        parentColumn = "token",
        entityColumn = "token"
    )
    val fullBatchParts: List<DbFullBatchPart>,

    @Relation(
        entity = DbDailyBatchPart::class,
        parentColumn = "token",
        entityColumn = "token"
    )
    val dailyBatchesParts: List<DbDailyBatchPart>
)

@Entity(
    tableName = "full_batch",
    indices = [
        Index("token")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbSession::class,
            parentColumns = ["token"],
            childColumns = ["token"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DbFullBatchPart(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var token: String = "", // will be set up during inserting to DB
    val batchNo: Int,
    val intervalStart: Long,
    val path: String
) : DbEntity

@Entity(
    tableName = "daily_batch",
    indices = [
        Index("token")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DbSession::class,
            parentColumns = ["token"],
            childColumns = ["token"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)

data class DbDailyBatchPart(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var token: String = "", // will be set up during inserting to DB
    val batchNo: Int,
    val intervalStart: Long,
    val path: String
) : DbEntity
