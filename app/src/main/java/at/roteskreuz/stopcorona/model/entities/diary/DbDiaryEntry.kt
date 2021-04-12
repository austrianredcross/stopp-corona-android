package at.roteskreuz.stopcorona.model.entities.diary

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import at.roteskreuz.stopcorona.skeleton.core.model.db.converters.EnumTypeConverter
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import org.threeten.bp.LocalDate

/**
 * Describes the event for the diary.
 */
@Entity(
    tableName = "diary_entry"
)
data class DbDiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val eventId: Long? = 0,
    val locationId: Long? = 0,
    val personId: Long? = 0,
    val publicTransportId: Long? = 0,
    val type : DiaryEntryType
) : DbEntity

data class DbDiaryEntryWrapper(
    @Embedded val dbDiaryEntry: DbDiaryEntry,
    @Relation(parentColumn = "eventId", entityColumn = "id")
    val dbEventEntity: DbDiaryEvent?,
    @Relation(parentColumn = "locationId", entityColumn = "id")
    val dbLocationEntity: DbDiaryLocation?,
    @Relation(parentColumn = "personId", entityColumn = "id")
    val dbPersonEntity: DbDiaryPerson?,
    @Relation(parentColumn = "publicTransportId", entityColumn = "id")
    val dbDiaryPublicTransportEntity: DbDiaryPublicTransport?
)

class DiaryEntryTypeConverter : EnumTypeConverter<DiaryEntryType>({ enumValueOf(it) })

enum class DiaryEntryType {
    EVENT, LOCATION, PERSON, PUBLIC_TRANSPORT
}