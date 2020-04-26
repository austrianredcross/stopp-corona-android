package at.roteskreuz.stopcorona.skeleton.core.model.api.converters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.threeten.bp.Instant

/**
 * Adapter to format between long and [Instant].
 */
object InstantAdapter {

    @FromJson
    fun fromJson(value: Long?): Instant? {
        return value?.let {
            Instant.ofEpochMilli(it)
        }
    }

    @ToJson
    fun toJson(value: Instant?): Long? {
        return value?.toEpochMilli()
    }
}