package at.roteskreuz.stopcorona.utils

import android.content.Context
import android.text.format.DateUtils
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Extension related to date and time.
 */

private val UTC_TIMEZONE: ZoneId
    get() = ZoneId.of("UTC")

/**
 * Get minutes difference between two times.
 */
fun ZonedDateTime.minutesTo(other: ZonedDateTime): Long {
    return Duration.between(this, other).toMinutes()
}

/**
 * Get duration between two times.
 */
operator fun ZonedDateTime.minus(other: ZonedDateTime): Duration {
    return Duration.between(this, other).abs()
}

/**
 * Get days difference between two dates.
 */
fun LocalDate.daysTo(other: LocalDate): Long {
    return abs(this.toEpochDay() - other.toEpochDay())
}

/**
 * Format date to display (month - text form if possible, year) based on natural locale style.
 */
fun LocalDate.formatMonthAndYear(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        DateUtils.FORMAT_NO_MONTH_DAY or
            DateUtils.FORMAT_SHOW_YEAR
    )
}

/**
 * Format date to display (day of month, month - text form if possible) based on natural locale style.
 */
fun LocalDate.formatDayAndMonth(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_NO_YEAR
    )
}

/**
 * Format date to display (day of month, month - text form if possible, year) based on natural locale style.
 */
fun LocalDate.formatDayAndMonthAndYear(context: Context): String {
    return atStartOfDay(ZoneId.systemDefault()).formatDayAndMonthAndYear(context)
}

/**
 * Format date to display (day of month, month - text form if possible, year) based on natural locale style.
 */
fun ZonedDateTime.formatDayAndMonthAndYear(context: Context, monthAsText: Boolean = true): String {
    val monthFormat = when {
        monthAsText -> DateUtils.FORMAT_ABBREV_MONTH
        else -> DateUtils.FORMAT_NUMERIC_DATE
    }

    return DateUtils.formatDateTime(
        context,
        toInstant().toEpochMilli(),
        monthFormat or
            DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_SHOW_YEAR
    )
}

/**
 * Format date to display (day of month, month - text form if possible, year, time) based on natural locale style.
 */
fun ZonedDateTime.formatDayAndMonthAndYearAndTime(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        toInstant().toEpochMilli(),
        DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_SHOW_YEAR or
            DateUtils.FORMAT_SHOW_TIME
    )
}

/**
 * Format to display time.
 */
fun ZonedDateTime.formatTime(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        toInstant().toEpochMilli(),
        DateUtils.FORMAT_SHOW_TIME
    )
}

/**
 * Format the [ZonedDateTime] as day and month.
 */
fun ZonedDateTime?.format(pattern: String): String? {
    val localDateFormatter = DateTimeFormatter.ofPattern(pattern)
    return this?.format(localDateFormatter)
}

/**
 * Format handshake times.
 */
fun ZonedDateTime.formatHandshakeLongVersion(context: Context): String {
    val roundedTime = minusMinutes(minute.toLong())
    fun String.withoutMinutes() = replace(":00", "")
    return context.string(R.string.infection_info_handshake_took_place_format,
        toLocalDate().formatDayAndMonth(context),
        roundedTime.formatTime(context).withoutMinutes(),
        roundedTime.plusHours(1).formatTime(context).withoutMinutes()
    )
}

/**
 * Format handshake times.
 */
fun ZonedDateTime.formatHandshakeShortVersion(context: Context): String {
    val roundedTime = minusMinutes(minute.toLong())
    return context.string(R.string.contact_history_time_interval,
        roundedTime.formatTime(context),
        roundedTime.plusHours(1).formatTime(context)
    )
}

/**
 * Compares the [ZonedDateTime] to the current timestamp.
 *
 * @return true is the given [ZonedDateTime] is in the future.
 */
fun ZonedDateTime.isInTheFuture(): Boolean {
    return this.isAfter(ZonedDateTime.now())
}

/**
 * Converts a unix timestamp to a rolling start interval number.
 */
fun ZonedDateTime.toRollingStartIntervalNumber(): Int {
    return (withZoneSameInstant(UTC_TIMEZONE).toEpochSecond() / Constants.ExposureNotification.INTERVAL_NUMBER_OFFSET.seconds).toInt()
}

/**
 * Converts a interval number from exposure notification framework to the [ZonedDateTime].
 */
fun Long.asExposureInterval(): ZonedDateTime {
    return ZonedDateTime.ofInstant(
        Instant.ofEpochSecond(this * Constants.ExposureNotification.INTERVAL_NUMBER_OFFSET.seconds),
        UTC_TIMEZONE
    )
}

/**
 * Returns start of the day of the provided [ZonedDateTime].
 */
fun ZonedDateTime.startOfTheDay(): ZonedDateTime {
    return truncatedTo(ChronoUnit.DAYS)
}

/**
 * Returns end of the day of the provided [ZonedDateTime].
 */
fun ZonedDateTime.endOfTheDay(): ZonedDateTime {
    return withHour(23).withMinute(59).withSecond(59)
}

/**
 * Evaluates if two dates are on the same calendar day
 */
fun ZonedDateTime.areOnTheSameDay(otherDateTime: ZonedDateTime): Boolean {
    return this.startOfTheDay().equals(otherDateTime.startOfTheDay())
}
