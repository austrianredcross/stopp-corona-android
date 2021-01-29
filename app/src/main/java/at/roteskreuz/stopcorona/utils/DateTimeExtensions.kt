package at.roteskreuz.stopcorona.utils

import android.content.Context
import android.text.format.DateUtils
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.model.exceptions.SilentError
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Get minutes difference between two times.
 */
fun ZonedDateTime.minutesTo(other: ZonedDateTime): Long {
    return Duration.between(this, other).toMinutes()
}

/**
 * Get milliseconds to other time.
 *
 * If other time is in the past return 0L
 */
fun ZonedDateTime.millisTo(other: ZonedDateTime): Long {
    return (other - this).toMillis().coerceAtLeast(0)
}

/**
 * Get milliseconds to other time.
 *
 * If other time is in the past return 0L
 */
fun LocalDateTime.millisTo(other: LocalDateTime): Long {
    return (other - this).toMillis().coerceAtLeast(0)
}

/**
 * Get [Duration] to other time.
 */
infix operator fun ZonedDateTime.minus(other: ZonedDateTime): Duration {
    return Duration.between(other, this)
}

/**
 * Get [Duration] to other time.
 */
infix operator fun LocalDateTime.minus(other: LocalDateTime): Duration {
    return Duration.between(other, this)
}

/**
 * Get days difference between two dates.
 *
 * If other day is in the past return 0L
 */
fun LocalDate.daysTo(other: LocalDate): Long {
    return (other.toEpochDay() - this.toEpochDay()).coerceAtLeast(0)
}

/**
 * Check if the time is between [startTime] and [endTime]
 */
fun LocalTime.isInBetween(startTime: LocalTime, endTime: LocalTime): Boolean {
    return this.isAfter(startTime) && this.isBefore(endTime)
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
 * Converts a unix timestamp to a rolling interval number.
 */
fun ZonedDateTime.toRollingIntervalNumber(): Int {
    return (toInstant().epochSecond / Constants.ExposureNotification.ROLLING_PERIOD_DURATION.seconds).toInt()
}

/**
 * Converts a unix timestamp to a rolling start interval number. I.e. the rolling interval number at
 * the start of the day.
 */
fun ZonedDateTime.toRollingStartIntervalNumber(): Int {
    return startOfTheUtcDay().toRollingIntervalNumber()
}

/**
 * Converts an interval number from exposure notification framework to an [Instant].
 */
fun Long.asExposureInterval(): ZonedDateTime {
    return ZonedDateTime.ofInstant(
        Instant.ofEpochSecond(this * Constants.ExposureNotification.ROLLING_PERIOD_DURATION.seconds),
        ZoneOffset.UTC
    )
}

/**
 * Checks the receiver if it is after [other]
 *
 * @param other
 * @return [this] if the [this] is after [other], else null
 */
fun ZonedDateTime.afterOrNull(other: ZonedDateTime): ZonedDateTime? {
    return if (isAfter(other)) {
        this
    } else {
        null
    }
}

/**
 * Returns start of the day of the provided [ZonedDateTime].
 */
fun ZonedDateTime.startOfTheDay(): ZonedDateTime = truncatedTo(ChronoUnit.DAYS)

/**
 * Returns start of the UTC day of the provided [ZonedDateTime].
 */
fun ZonedDateTime.startOfTheUtcDay(): ZonedDateTime {
    val startOfUtcDay = toInstant().truncatedTo(ChronoUnit.DAYS)
    return ZonedDateTime.ofInstant(startOfUtcDay, zone)
}

/**
 * Returns end of the day of the provided [ZonedDateTime].
 */
fun ZonedDateTime.endOfTheDay(): ZonedDateTime {
    return startOfTheDay().plusDays(1).minusNanos(1)
}

/**
 * Returns end of the UTC day of the provided [ZonedDateTime].
 */
fun ZonedDateTime.endOfTheUtcDay(): ZonedDateTime {
    val endOfTheUtcDay = toInstant().truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1)
    return ZonedDateTime.ofInstant(endOfTheUtcDay, zone)
}

/**
 * Returns start of the UTC day of the provided [Instant].
 */
fun Instant.startOfTheUtcDay(): Instant = truncatedTo(ChronoUnit.DAYS)

/**
 * Returns end of the UTC day of the provided [Instant].
 */
fun Instant.endOfTheUtcDay(): Instant {
    return truncatedTo(ChronoUnit.DAYS).plusDays(1).minusNanos(1)
}

/**
 * Evaluates if two dates are on the same calendar day.
 *
 * Both dates must be in the same time zone! Otherwise it is not clear when the day starts.
 */
fun ZonedDateTime.areOnTheSameDay(otherDateTime: ZonedDateTime): Boolean {
    if (zone != otherDateTime.zone) {
        Timber.e(SilentError(IllegalArgumentException("areOnTheSameDay called with different time zones. This does not make sense")))
    }
    return startOfTheDay() == otherDateTime.startOfTheDay()
}

/**
 * Evaluates if two dates are on the same calendar day in UTC.
 *
 * Two null-Instances are considered to be on the same day.
 */
fun Instant?.areOnTheSameUtcDay(otherDateTime: Instant?): Boolean {
    return this?.startOfTheUtcDay() == otherDateTime?.startOfTheUtcDay()
}

/**
 * Add days to [Instant]
 *
 * Provides method known from ZonedDateTime
 */
fun Instant.plusDays(days: Long): Instant = plus(days, ChronoUnit.DAYS)

/**
 * Subtracts days from [Instant]
 *
 * Provides method known from ZonedDateTime
 */
fun Instant.minusDays(days: Long): Instant = minus(days, ChronoUnit.DAYS)

/**
 * Format the [Instant]
 */
fun Instant?.format(pattern: String): String? {
    val localDateFormatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
    return localDateFormatter.format(this)
}

/**
 * Format the [LocalDate]
 */
fun LocalDate?.format(pattern: String): String? {
    val localDateFormatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
    return localDateFormatter.format(this)
}

fun ZonedDateTime.millisToNextUtcDay(): Long {
    val nextDay = this.plusDays(1)
    val startOfTheNextUtcDay = nextDay.startOfTheUtcDay()
    return this.millisTo(startOfTheNextUtcDay)
}
/**
 * Format the [Date] to String
 */
fun Date.toString(format: String): String {
    val dateFormatter = SimpleDateFormat(format)
    return dateFormatter.format(this)
}

/**
 * Get days difference between two times.
 */
fun ZonedDateTime.daysTo(other: ZonedDateTime): Long {
    return Duration.between(this, other).toDays()
}