package at.roteskreuz.stopcorona.model.entities.discovery

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime

/**
 * Describes automatic BT discovery event.
 */
@Entity(
    tableName = "automatic_discovery",
    indices = [
        Index("publicKey")
    ]
)
@Parcelize
data class DbAutomaticDiscoveryEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val publicKey: ByteArray,
    val proximity: Int,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime? = null
) : DbEntity, Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbAutomaticDiscoveryEvent) return false

        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (proximity != other.proximity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startTime.hashCode()
        result = 31 * result + (endTime?.hashCode() ?: 0)
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + proximity
        return result
    }
}
