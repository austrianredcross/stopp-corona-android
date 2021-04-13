package at.roteskreuz.stopcorona.model.entities.diary

import androidx.room.*
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import org.threeten.bp.LocalDate

/**
 * Describes a contact person for the diary.
 */
@Entity(
    tableName = "diary_person"
)
data class DbDiaryPerson(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val notes: String?
) : DbEntity