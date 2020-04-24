package at.roteskreuz.stopcorona.skeleton.core.model.api.converters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

/**
 * Adapter to format between long and [LocalDate].
 */
object LocalDateAdapter {

    @FromJson
    fun fromJson(value: String?): LocalDate? {
        return value?.let {
            LocalDate.parse(it)
        }
    }

    @ToJson
    fun toJson(value: LocalDate?): String? {
        return value?.let {
            DateTimeFormatter.ISO_LOCAL_DATE.format(it)
        }
    }
}