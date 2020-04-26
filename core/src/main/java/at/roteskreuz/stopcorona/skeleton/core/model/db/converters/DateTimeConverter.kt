package at.roteskreuz.stopcorona.skeleton.core.model.db.converters

import androidx.room.TypeConverter
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Converter for dateTime using [ZonedDateTime] or [LocalDate].
 */
class DateTimeConverter {

    @TypeConverter
    fun timestampToDateTime(timestamp: Long?): ZonedDateTime? =
        timestamp?.let { ZonedDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.of("UTC")) }

    @TypeConverter
    fun dateTimeToTimestamp(date: ZonedDateTime?): Long? = date?.withZoneSameInstant(ZoneId.of("UTC"))?.toEpochSecond()

    @TypeConverter
    fun timestampToDate(timestamp: Int?): LocalDate? =
        timestamp?.let { LocalDate.of(it / 10000, (it / 100) % 100, it % 100) } //yyyyMMdd

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Int? = date?.run { year * 10000 + monthValue * 100 + dayOfMonth }
}