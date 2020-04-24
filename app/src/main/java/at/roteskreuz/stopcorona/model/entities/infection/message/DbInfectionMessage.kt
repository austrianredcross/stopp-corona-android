package at.roteskreuz.stopcorona.model.entities.infection.message

import android.os.Parcelable
import androidx.room.*
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime
import java.util.UUID

/**
 * Describes a sent infection message.
 */
@Entity(
    tableName = "infection_message",
    indices = [
        Index("isReceived")
    ]
)
@Parcelize
data class DbInfectionMessage(
    val messageType: MessageType,
    val timeStamp: ZonedDateTime,
    val isReceived: Boolean = false,
    @PrimaryKey
    val uuid: UUID = UUID.randomUUID()
) : DbEntity, Parcelable

/**
 * Contains the contacts to which an infection message has been sent.
 */
@Entity(
    tableName = "contact_with_infection_message",
    foreignKeys = [
        ForeignKey(
            entity = DbInfectionMessage::class,
            parentColumns = ["uuid"],
            childColumns = ["messageUuid"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
@Parcelize
data class DbContactWithInfectionMessage(
    @PrimaryKey
    val messageUuid: UUID,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val publicKey: ByteArray
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbContactWithInfectionMessage) return false

        if (messageUuid != other.messageUuid) return false
        if (!publicKey.contentEquals(other.publicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = messageUuid.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        return result
    }
}

@Parcelize
data class DbInfectionFullContainer(
    @Embedded
    var infectionMessageInstance: DbInfectionMessage,

    @Relation(
        entity = DbContactWithInfectionMessage::class,
        parentColumn = "uuid",
        entityColumn = "messageUuid"
    )
    var contacts: List<DbContactWithInfectionMessage> = listOf()
) : Parcelable

class MessageTypeConverter {
    @TypeConverter
    fun toIdentifier(messageType: MessageType?): String? {
        return messageType?.identifier
    }

    @TypeConverter
    fun fromIdentifier(identifier: String?): MessageType? {
        return identifier?.let { MessageType(it) }
    }
}

/**
 * Converter from and to UUID.
 */
class UUIDConverter {

    @TypeConverter
    fun stringToUUID(value: String?): UUID? =
        value?.let { UUID.fromString(it) }

    @TypeConverter
    fun uuidToString(uuid: UUID?): String? = uuid?.toString()
}
