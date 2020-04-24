package at.roteskreuz.stopcorona.model.entities.infection.message

import android.os.Parcelable
import at.roteskreuz.stopcorona.constants.Constants.Security.BLOCK_SIZE
import at.roteskreuz.stopcorona.model.entities.infection.info.WarningType
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import at.roteskreuz.stopcorona.skeleton.core.model.db.converters.DateTimeConverter
import at.roteskreuz.stopcorona.skeleton.core.model.entities.ApiEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.util.UUID

/**
 * Describes infection messages.
 */
@JsonClass(generateAdapter = true)
data class ApiInfectionMessages(
    @field:Json(name = "infection-messages")
    val infectionMessages: List<ApiInfectionMessage>?
)

@JsonClass(generateAdapter = true)
data class ApiInfectionMessage(
    val id: Long,
    val message: String
)

/**
 * Message types informing about different states.
 */
sealed class MessageType(val identifier: String) : Parcelable {

    companion object {
        operator fun invoke(identifier: String): MessageType? {
            return when (identifier) {
                InfectionLevel.Red.identifier -> InfectionLevel.Red
                InfectionLevel.Yellow.identifier -> InfectionLevel.Yellow
                Revoke.identifier -> Revoke
                else -> {
                    Timber.e(SilentError("Unknown message type identifier $identifier"))
                    null
                }
            }
        }
    }

    abstract val warningType: WarningType

    /**
     * Levels of infections by source of diagnose.
     */
    sealed class InfectionLevel(identifier: String) : MessageType(identifier) {

        /**
         * Diagnosed by doctor.
         */
        @Parcelize
        object Red : InfectionLevel("r") {

            @IgnoredOnParcel
            override val warningType: WarningType = WarningType.RED
        }

        /**
         * Diagnosed by user.
         */
        @Parcelize
        object Yellow : InfectionLevel("y") {

            @IgnoredOnParcel
            override val warningType: WarningType = WarningType.YELLOW
        }
    }

    /**
     * User is recovered as result of self test.
     */
    @Parcelize
    object Revoke : MessageType("g") {

        @IgnoredOnParcel
        override val warningType: WarningType = WarningType.REVOKE
    }
}

/**
 * Encapsulates the payload of an infection warning message.
 *
 * Instances can be serialized for encryption with [toByteArray] and deserialized with the
 * constructor like syntax of the companion object's invoke method.
 *
 * Example:
 * val infectionMessageContent = InfectionMessageContent(MessageType.r, System.currentTimeMillis() / 1000L)
 * val serializedByteArray = infectionMessageContent.toByteArray()
 * val deserializedInfectionMessageContent = InfectionMessageContent(serializedByteArray)
 *
 * @property messageType
 * @property timeStamp
 * @property uuid
 */
data class InfectionMessageContent(
    val messageType: MessageType,
    val timeStamp: ZonedDateTime,
    val uuid: UUID = UUID.randomUUID()
) : ApiEntity<DbInfectionMessage> {

    /**
     * Serialize to byte array of size [BLOCK_SIZE]
     *
     * Warning: Timestamp will lose minutes, seconds and milliseconds information.
     *
     * @return serialized InfectionMessageContent
     */
    fun toByteArray(): ByteArray {
        val padding = ByteArray(BLOCK_SIZE - Byte.SIZE_BYTES - Long.SIZE_BYTES - uuidBytes)
        val typeArray = messageType.identifier.toByteArray()
        val timeStampArray = timeStamp
            .truncatedTo(ChronoUnit.HOURS) // set minutes = 00, seconds = 00, milliseconds = 000
            .toEpochSecond()
            .toByteArray()
        val uuidArray = uuid.toByteArray()
        return padding + typeArray + timeStampArray + uuidArray
    }

    companion object {

        /**
         * Deserialize from byte array
         *
         * @param byteArray
         * @return InfectionMessageContent instantiated from [byteArray] or null if the byte array
         *          is not a valid InfectionMessageContent
         */
        operator fun invoke(byteArray: ByteArray): InfectionMessageContent? {
            try {
                val uuidStart = BLOCK_SIZE - uuidBytes
                val timeStampStart = uuidStart - Long.SIZE_BYTES
                val messageTypeStart = timeStampStart - Byte.SIZE_BYTES
                val paddingStart = 0

                val paddingArray = byteArray.sliceArray(paddingStart until messageTypeStart)
                val messageTypeByte = byteArray[messageTypeStart]
                val timeStampArray = byteArray.sliceArray(timeStampStart until uuidStart)
                val uuidArray = byteArray.sliceArray(uuidStart until byteArray.size)

                val messageType = messageTypeByte.toChar().toString()
                val timeStamp = DateTimeConverter().timestampToDateTime(timeStampArray.toLong())!!
                val uuid = uuidArray.toUuid()

                val nullByte = 0.toByte()
                if (paddingArray.firstOrNull { it != nullByte } != null) {
                    return null
                }

                return InfectionMessageContent(MessageType(messageType)!!, timeStamp, uuid)
            } catch (e: Exception) {
                Timber.e(SilentError(e))
                return null
            }
        }

        private val uuidBytes
            get() = 2 * Long.SIZE_BYTES

        private fun Long.toByteArray() =
            ByteArray(Long.SIZE_BYTES) { currentByte -> (this.shr(Byte.SIZE_BITS * (7 - currentByte)) and 0xff).toByte() }

        private fun UUID.toByteArray() = mostSignificantBits.toByteArray() + leastSignificantBits.toByteArray()

        private fun ByteArray.toLong(): Long {
            var long = 0L
            this.forEach {
                long = long.shl(8) + (it.toLong().and(0xff))
            }

            return long
        }

        private fun ByteArray.toUuid(): UUID {
            val msbArray = sliceArray(0 until Long.SIZE_BYTES)
            val lsbArray = sliceArray(Long.SIZE_BYTES until 2 * Long.SIZE_BYTES)

            return UUID(msbArray.toLong(), lsbArray.toLong())
        }
    }

    override fun asDbEntity(): DbInfectionMessage {
        return DbInfectionMessage(
            messageType = messageType,
            timeStamp = timeStamp,
            uuid = uuid
        )
    }
}