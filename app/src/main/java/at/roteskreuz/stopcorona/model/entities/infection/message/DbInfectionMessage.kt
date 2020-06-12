package at.roteskreuz.stopcorona.model.entities.infection.message

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import org.threeten.bp.ZonedDateTime
import java.util.*

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
