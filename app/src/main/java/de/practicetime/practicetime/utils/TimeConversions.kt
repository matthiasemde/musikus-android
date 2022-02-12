package de.practicetime.practicetime.utils

import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField

const val SECONDS_PER_HOUR = 60 * 60
const val SECONDS_PER_DAY = 60 * 60 * 24

/** Format Time intelligently, e.g. "1h 30m" or "< 1m" */
const val TIME_FORMAT_HUMAN_PRETTY = 0

/** Like TIME_FORMAT_HUMAN_PRETTY, but without */
const val TIME_FORMAT_HUMAN_PRETTY_SHORT = 1

/** Format Time rounded to next full day / hour / minute / second, e.g. "22 hours", "32 minutes */
const val TIME_FORMAT_PRETTY_APPROX = 2

/** Same as TIME_FORMAT_PRETTY_APPROX, but use "days", "h" and "m" instead of "days"/"hours"/"minutes" */
const val TIME_FORMAT_PRETTY_APPROX_SHORT = 4

/** Fixed format HH:MM:SS */
const val TIME_FORMAT_HMS_DIGITAL = 3

/** The scaling factor of 'h' an 'm' in time strings for smaller text. */
const val SCALE_FACTOR_FOR_SMALL_TEXT = 0.8f

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

/**
 * Get the formatted string for a duration of seconds, e.g. "1h 32m"
 *
 * The 'h' and 'm' are shrunk, if used. In that case,
 * the return value is a SpannableString which can be used directly in e.g. TextViews.
 * If you want to concat the returned CharSequence with another String, do it with
 * TextUtils.concat() to preserve styling!
 * If you NEED a String, simply call .toString().
 *
 * @param durationSeconds the duration in seconds to be converted
 * @param format one of TIME_FORMAT_XXX variables, indicating the output format
 * @return A CharSequence (either a String or a SpannableString) with the final String.
 */
fun getDurationString(durationSeconds: Int, format: Int, scale: Float = 0.6f): CharSequence {
    val (hours, minutes, seconds) = secondsDurationToHoursMinSec(durationSeconds)
    val days = secondsDurationToFullDays(durationSeconds)

    val spaceOrNot = when(format) {
        TIME_FORMAT_HUMAN_PRETTY -> " "
        TIME_FORMAT_HUMAN_PRETTY_SHORT -> ""
        else -> ""
    }

    when (format) {
        TIME_FORMAT_HUMAN_PRETTY, TIME_FORMAT_HUMAN_PRETTY_SHORT -> {
            val str =
                if (hours > 0 && minutes > 0) {
                    ("%dh" + spaceOrNot + "%02dm").format(hours, minutes)
                } else if (hours > 0 && minutes == 0) {
                    "%dh".format(hours)
                } else if (minutes == 0 && durationSeconds > 0) {
                    "<" + spaceOrNot + "1m"
                } else {
                    ("%dm").format(minutes)
                }
            return getSpannableHourMinShrunk(str, scale)
        }

        TIME_FORMAT_HMS_DIGITAL -> {
            return "%02d:%02d:%02d".format(
                hours,
                minutes,
                seconds
            )
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

        TIME_FORMAT_PRETTY_APPROX_SHORT -> {
            return when {
                days > 1 -> {
                    // if time left is larger than a day, show the number of begun days
                    "${days + 1} days"
                }
                hours > 1 -> {
                    // show the number of begun hours
                    "${hours + 1}h"
                }
                else -> {
                    // show the number of begun minutes
                    "${minutes + 1}m"
                }
            }
        }

        else -> {
            return "TIME_FORMAT_ERR"
        }
    }
}

/**
 * Helper function to shrink the 'h' and the 'm' or 'min' in a time sting like e.g. "3h 42min"
 *
 * @param str the whole time string, e.g. "3h 42m"
 */
private fun getSpannableHourMinShrunk(str: String, scaleFactor: Float = 0.6f): CharSequence {
    val spannable = SpannableString(str)
    val hIndex = str.indexOf('h')
    if (hIndex >= 0) {
        // shrink the 'h'
        spannable.setSpan(RelativeSizeSpan(scaleFactor), hIndex, hIndex + 1, 0)
    }
    val mIndex = str.indexOf('m')
    if (mIndex >= 0) {
        // shrink 'm' or "min", accordingly
        spannable.setSpan(RelativeSizeSpan(scaleFactor), mIndex, mIndex + 1, 0)
    }
    return spannable
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