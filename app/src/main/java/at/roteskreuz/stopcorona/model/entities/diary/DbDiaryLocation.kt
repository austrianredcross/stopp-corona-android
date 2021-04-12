package at.roteskreuz.stopcorona.model.entities.diary

import androidx.room.*
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import org.threeten.bp.LocalDate

/**
 * Describes the place for the diary.
 */
@Entity(
    tableName = "diary_location"
)
data class DbDiaryLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val locationName: String,
    val timeOfDay: String?
) : DbEntity