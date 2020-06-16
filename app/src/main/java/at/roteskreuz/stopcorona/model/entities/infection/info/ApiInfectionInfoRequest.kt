package at.roteskreuz.stopcorona.model.entities.infection.info

import at.roteskreuz.stopcorona.skeleton.core.model.db.converters.EnumTypeConverter
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

enum class WarningType(val transmissionRiskLevel: Int) {
    @field:Json(name = "yellow-warning")
    YELLOW(5),

    @field:Json(name = "red-warning")
    RED(2),

    @field:Json(name = "green-warning")
    GREEN(6)
}

class WarningTypeConverter : EnumTypeConverter<WarningType>({ enumValueOf(it) })

/**
 * Custom annotation to proper convert non standard local dates.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@JsonQualifier
annotation class NotIsoLocalDate

/**
 * Adapter to format date by server specifics.
 */
object LocalDateNotIsoAdapter {

    private val format = DateTimeFormatter.ofPattern("dd.MM.YYYY")

    @FromJson
    @NotIsoLocalDate
    fun fromJson(value: String?): LocalDate? {
        return value?.let {
            LocalDate.parse(it, format)
        }
    }

    @ToJson
    fun toJson(@NotIsoLocalDate value: LocalDate?): String? {
        return value?.let {
            format.format(it)
        }
    }
}