package at.roteskreuz.stopcorona.skeleton.core.model.api.converters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Formats dates using [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt), which is
 * formatted like `2015-09-26T18:23:50.250Z`.
 */
object Rfc3339ZonedDateTimeAdapter {

    @FromJson
    fun fromJson(value: String?): ZonedDateTime? {
        return value?.let {
            ZonedDateTime.parse(it)
        }
    }

    @ToJson
    fun toJson(value: ZonedDateTime?): String? {
        return value?.let {
            DateTimeFormatter.ISO_ZONED_DATE_TIME.format(it)
        }
    }
}