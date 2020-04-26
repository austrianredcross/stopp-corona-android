package at.roteskreuz.stopcorona.model.entities.nearby

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.roteskreuz.stopcorona.model.services.CoronaDetectionService
import org.threeten.bp.ZonedDateTime

/**
 * Describes contact data of nearby result with timestamp of meeting.
 */
@Entity(
    tableName = "nearby_record"
)
data class DbNearbyRecord(
    @PrimaryKey()
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val publicKey: ByteArray,
    val timestamp: ZonedDateTime = ZonedDateTime.now(),
    /**
     * True if detected by [CoronaDetectionService],
     * False if detected by manual handshake.
     */
    val detectedAutomatically: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbNearbyRecord) return false

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (timestamp != other.timestamp) return false
        if (detectedAutomatically != other.detectedAutomatically) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + detectedAutomatically.hashCode()
        return result
    }
}