package at.roteskreuz.stopcorona.model.entities.infection.message

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime
import java.util.UUID

/**
 * Describes a received infection message.
 */
@Entity(
    tableName = "received_infection_message"
)
data class DbReceivedInfectionMessage(
    @PrimaryKey
    val uuid: UUID = UUID.randomUUID(),
    val messageType: MessageType,
    val timeStamp: ZonedDateTime
) : DbEntity

/**
 * Describes a sent infection message.
 */
@Entity(
    tableName = "sent_infection_message"
)
@Parcelize
data class DbSentInfectionMessage(
    @PrimaryKey
    val uuid: UUID = UUID.randomUUID(),
    val messageType: MessageType,
    val timeStamp: ZonedDateTime,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val publicKey: ByteArray
) : DbEntity, Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbSentInfectionMessage) return false

        if (uuid != other.uuid) return false
        if (messageType != other.messageType) return false
        if (timeStamp != other.timeStamp) return false
        if (!publicKey.contentEquals(other.publicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + messageType.hashCode()
        result = 31 * result + timeStamp.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        return result
    }
}

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
