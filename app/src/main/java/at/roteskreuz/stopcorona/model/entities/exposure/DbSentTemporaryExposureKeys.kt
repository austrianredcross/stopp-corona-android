package at.roteskreuz.stopcorona.model.entities.exposure

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.roteskreuz.stopcorona.skeleton.core.model.entities.DbEntity
import kotlinx.android.parcel.Parcelize
import java.util.*

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
    val password: UUID
) : DbEntity, Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbSentTemporaryExposureKeys) return false

        if (rollingStartIntervalNumber != other.rollingStartIntervalNumber) return false
        if (password != other.password) return false

        return true
    }

    override fun hashCode(): Int {
        var result = password.hashCode()
        result = 31 * result + rollingStartIntervalNumber.hashCode()
        return result
    }
}