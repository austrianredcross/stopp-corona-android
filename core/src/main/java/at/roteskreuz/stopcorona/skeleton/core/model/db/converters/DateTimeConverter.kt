package at.roteskreuz.stopcorona.skeleton.core.model.db.converters

import androidx.room.TypeConverter
import org.threeten.bp.*

/**
 * Converter for dateTime using [Instant], [ZonedDateTime] or [LocalDate].
 */
class DateTimeConverter {

    @TypeConverter
    fun timestampToDateTime(timestamp: Long?): ZonedDateTime? =
        timestamp?.let { ZonedDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()) }

    @TypeConverter
    fun dateTimeToTimestamp(date: ZonedDateTime?): Long? = date?.withZoneSameInstant(ZoneOffset.UTC)?.toEpochSecond()

    @TypeConverter
    fun timestampToInstant(timestamp: Long?): Instant? = timestamp?.let { Instant.ofEpochSecond(it) }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? = instant?.epochSecond

    @TypeConverter
    fun timestampToDate(timestamp: Int?): LocalDate? =
        timestamp?.let { LocalDate.of(it / 10000, (it / 100) % 100, it % 100) } //yyyyMMdd

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Int? = date?.run { year * 10000 + monthValue * 100 + dayOfMonth }

    @TypeConverter
    fun timestampToTime(timestamp: Int?): LocalTime? =
        timestamp?.let { LocalTime.of(it / (1000 * 60 * 60), (it / (1000 * 60)) % 60, (it / 1000) % 60) }

    @TypeConverter
    fun timeToTimestamp(time: LocalTime?): Int? = time?.run { hour * 1000 * 60 * 60 + minute * 1000 * 60 + second * 1000}
}