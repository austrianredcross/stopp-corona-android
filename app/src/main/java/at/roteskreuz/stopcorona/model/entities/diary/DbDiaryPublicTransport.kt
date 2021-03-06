package at.roteskreuz.stopcorona.model.entities.diary

import org.threeten.bp.LocalTime
import androidx.room.*
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

/**
 * Describes the event for the diary.
 */
@Entity(
    tableName = "diary_public_transport"
)
data class DbDiaryPublicTransport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val startLocation: String?,
    val endLocation: String?,
    val startTime: LocalTime?
) : DbEntity