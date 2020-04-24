package at.roteskreuz.stopcorona.utils

import android.content.Context
import android.text.format.DateUtils
import at.roteskreuz.stopcorona.R
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Extension related to date and time.
 */

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
