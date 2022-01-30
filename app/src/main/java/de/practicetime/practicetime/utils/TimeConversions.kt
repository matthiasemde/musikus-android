package de.practicetime.practicetime.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField

const val SECONDS_PER_DAY = 60 * 60 * 24

/** Format Time intelligently, e.g. "1h 30min" or "<1m" */
const val TIME_FORMAT_HUMAN_PRETTY = 0

/** Like TIME_FORMAT_HUMAN_PRETTY, but with "m" instead of "min" */
const val TIME_FORMAT_HUMAN_PRETTY_SHORT = 1

/** Format Time rounded to next full day / hour / minute / second, e.g. "22 hours", "32 minutes */
const val TIME_FORMAT_PRETTY_APPROX = 2

/** Fixed format HH:MM:SS */
const val TIME_FORMAT_HMS_DIGITAL = 3

/** Fixed format HH:MM for >1h, else e.g. "32 min" for <1h (used in GoalsProgressBar) */
const val TIME_FORMAT_HM_DIGITAL_OR_MIN_HUMAN = 4

/**
 * DateFormatter Patterns for DateTimeFormatter(). Reference: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
 */

/** format string for DateTimeFormatter() resulting in DD.MM format, e.g. "31.01." (for 31st January) */
const val DATE_FORMATTER_PATTERN_DAY_AND_MONTH = "dd.MM."

/** format string for DateTimeFormatter() for one-letter weekday abbrev., e.g. "M" for Monday */
const val DATE_FORMATTER_PATTERN_WEEKDAY_SHORT = "EEEEE"

/** format string for DateTimeFormatter() for weekday abbrev., e.g. "Mon" for Monday */
const val DATE_FORMATTER_PATTERN_WEEKDAY_ABBREV = "E"

/** format string for DateTimeFormatter() for "January", "February", etc. */
const val DATE_FORMATTER_PATTERN_MONTH_TEXT_FULL = "MMMM"

/** format string for DateTimeFormatter() for "Jan", "Feb", "Jul", etc. */
const val DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV = "MMM"

/** format string for DateTimeFormatter() for day of month (not zero-padded), e.g. "6", "12", etc. */
const val DATE_FORMATTER_PATTERN_DAY_OF_MONTH = "d"

/** format string for DateTimeFormatter() for day of month (zero-padded), e.g. "06", "12", etc. */
const val DATE_FORMATTER_PATTERN_DAY_OF_MONTH_PADDED = "dd"

/** format string for DateTimeFormatter() for year, e.g. "2022" */
const val DATE_FORMATTER_PATTERN_YEAR = "y"

fun secondsDurationToHoursMinSec(totalSeconds: Int): Triple<Int, Int, Int> {
    val hours =  totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return Triple(hours, minutes, seconds)
}

fun secondsDurationToFullDays(seconds: Int): Int {
    return seconds / SECONDS_PER_DAY
}

fun getDurationString(durationSeconds: Int, format: Int) : String {
    val (hours, minutes, seconds) = secondsDurationToHoursMinSec(durationSeconds)
    val days = secondsDurationToFullDays(durationSeconds)

    val minutesName = when(format) {
        TIME_FORMAT_HUMAN_PRETTY -> "min"
        TIME_FORMAT_HUMAN_PRETTY_SHORT -> "m"
        else -> ""
    }

    when (format) {
        TIME_FORMAT_HUMAN_PRETTY, TIME_FORMAT_HUMAN_PRETTY_SHORT -> {
            return if (hours > 0) {
                "%dh %d$minutesName".format(hours, minutes)
            } else if (minutes == 0 && durationSeconds > 0) {
                "<1$minutesName"
            } else {
                "%d$minutesName".format(minutes)
            }
        }

        TIME_FORMAT_HMS_DIGITAL -> {
            return "%02d:%02d:%02d".format(
                hours,
                minutes,
                seconds
            )
        }
        TIME_FORMAT_HM_DIGITAL_OR_MIN_HUMAN -> {
            return when {
                hours > 0 -> {
                    "%02d:%02d".format(hours, minutes)
                }
                minutes > 0 -> {
                    "%d min".format(minutes)
                }
                else -> {
                    "< 1min"
                }
            }
        }
        TIME_FORMAT_PRETTY_APPROX -> {
            return when {
                days > 1 -> {
                    // if time left is larger than a day, show the number of begun days
                    "${days + 1} days"
                }
                hours > 1 -> {
                    // show the number of begun hours
                    "${hours + 1} hours"
                }
                else -> {
                    // show the number of begun minutes
                    "${minutes + 1} minutes"
                }
            }
        }
        else -> {
            return "TIME_FORMAT_ERR"
        }
    }
}

fun getCurrTimestamp(): Long {
    return Instant.now().epochSecond
}

/**
 * Get the Beginning of dayOffset Days from now. dayOffset>0 -> future, dayOffset<0 -> past
 */
fun getStartOfDay(dayOffset: Long): ZonedDateTime {
    return ZonedDateTime.now()
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
        .plusDays(dayOffset)
}

/**
 * Get the End of dayOffset Days from now. Half-open: Actually get the Start of the Next day
 */
fun getEndOfDay(dayOffset: Long): ZonedDateTime {
    return getStartOfDay(dayOffset + 1)
}

/**
 * get the Beginning of a Day (1=Mo, 7=Sun) of the current week (weekOffset=0) / the weeks before (weekOffset<0)
  */
fun getStartOfDayOfWeek(dayIndex: Long, weekOffset: Long): ZonedDateTime {
    return ZonedDateTime.now()
        .with(ChronoField.DAY_OF_WEEK , dayIndex )         // ISO 8601, Monday is first day of week.
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
        .plusWeeks(weekOffset)
}

/**
 * get the End of a Day (1=Mo, 7=Sun) of the current week (weekOffset=0) / the weeks before (weekOffset<0)
 */
fun getEndOfDayOfWeek(dayIndex: Long, weekOffset: Long): ZonedDateTime {
    // because of half-open approach we have to get the "start of the _next_ day" instead of the end of the current day
    // e.g. end of Tuesday = Start of Wednesday, so make dayIndex 2 -> 3
    var nextDay = dayIndex + 1
    var weekOffsetAdapted = weekOffset
    if (dayIndex > 6) {
        nextDay = (dayIndex + 1) % 7
        weekOffsetAdapted += 1  // increase weekOffset so that we take the start of the first day of NEXT week as end of day
    }
    return getStartOfDayOfWeek(nextDay, weekOffsetAdapted)
}

/**
 * gets start date of a Week
 */
fun getStartOfWeek(weekOffset: Long): ZonedDateTime {
    return getStartOfDayOfWeek(1, weekOffset)
}

/**
 * gets end date of a Week
  */
fun getEndOfWeek(weekOffset: Long): ZonedDateTime {
    return getEndOfDayOfWeek(7, weekOffset)
}

/**
 * returns the weekDay of today from index 1=Mo until 7=Sun
  */
fun getCurrentDayIndexOfWeek(): Int {
    return ZonedDateTime.now()
        .toLocalDate().dayOfWeek.value
}

/**
 * Get the start of a month.
 * monthOffset=0 ->, monthOffset=1 -> next month, monthOffset = -1 -> last month, etc.
 */
fun getStartOfMonth(monthOffset: Long): ZonedDateTime {
    return ZonedDateTime.now()
        .with(ChronoField.DAY_OF_MONTH , 1 )    // jump to first day of this month
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
        .plusMonths(monthOffset) // add desired number of months from now
}

/**
 * Get the end of a month. Half-open: Actually get the Start of the next month.
 */
fun getEndOfMonth(monthOffset: Long): ZonedDateTime {
    return getStartOfMonth(monthOffset + 1)
}

/**
 * Convert epoch seconds to ZonedDateTime with correct TimeZone
 */
fun epochSecondsToDate(epochSecs: Long): ZonedDateTime {
    return ZonedDateTime
        .ofInstant(Instant.ofEpochSecond(epochSecs), ZoneId.systemDefault())
}