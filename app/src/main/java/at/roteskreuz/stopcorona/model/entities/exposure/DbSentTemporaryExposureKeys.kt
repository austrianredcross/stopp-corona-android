package at.roteskreuz.stopcorona.model.entities.exposure

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import at.roteskreuz.stopcorona.model.entities.infection.message.MessageType
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import kotlinx.android.parcel.Parcelize
import java.util.UUID

/**
 * Describes a sent temporary exposure key through it's associated rolling start interval number
 * and password.
 */
@Entity(
    tableName = "sent_temporary_exposure_keys"
)
@Parcelize
data class DbSentTemporaryExposureKeys(
    @PrimaryKey
    val rollingStartIntervalNumber: Int,
    val password: UUID,
    val messageType: MessageType
) : DbEntity, Parcelable

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

/**
 * Converter from and to [MessageType].
 */
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