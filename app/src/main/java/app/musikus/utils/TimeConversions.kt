/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package app.musikus.utils

import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

typealias Timeframe = Pair<ZonedDateTime, ZonedDateTime>

const val SECONDS_PER_HOUR = 60 * 60
const val SECONDS_PER_DAY = 60 * 60 * 24

enum class TimeFormat {

    /** Format Time intelligently, e.g. "1h 30m" or "< 1m" */
    HUMAN_PRETTY,

    /** Like TimeFormat.HUMAN_PRETTY, but without */
    HUMAN_PRETTY_SHORT,

    /** Format Time rounded to next full day / hour / minute / second, e.g. "22 hours", "32 minutes */
    PRETTY_APPROX,

    /** Same as TimeFormat.PRETTY_APPROX, but use "days", "h" and "m" instead of "days"/"hours"/"minutes" */
    PRETTY_APPROX_SHORT,

    /** Fixed format HH:MM:SS */
    HMS_DIGITAL,

    /** Fixed format MM:SS */
    MS_DIGITAL,

    /** Fixed format HH:MM for >1h, else e.g. "32 min" for <1h (used in GoalsProgressBar) */
    HM_DIGITAL_OR_MIN_HUMAN;
}

/** The scaling factor of 'h' an 'm' in time strings for smaller text. */
const val SCALE_FACTOR_FOR_SMALL_TEXT = 0.8f

/**
 * Reference for DateFormatter patterns : https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
 */

enum class DateFormat {
    /** hours and minutes of day with offset when in another timezone, e.g. "6:02", "12:13-2:00"  */
    TIME_OF_DAY,

    /** DD.MM.YYYY format, e.g. "31.01.2023" (for 31st January 2023) */
    DAY_MONTH_YEAR,

    /** day of month (not zero-padded), e.g. "6", "12", etc. */
    DAY_OF_MONTH,

    /** day of month (zero-padded), e.g. "06", "12", etc. */
    DAY_OF_MONTH_PADDED,

    /** DD.MM format, e.g. "31.01." (for 31st January) */
    DAY_AND_MONTH,

    /** weekday abbrev., e.g. "Mon" for Monday */
    WEEKDAY_ABBREVIATED,

    /** one-letter weekday abbrev., e.g. "M" for Monday */
    WEEKDAY_SINGLE_LETTER,

    /** "January", "February", etc. */
    MONTH,

    /** "Jan", "Feb", "Jul", etc. */
    MONTH_ABBREVIATED,

    /** year, e.g. "2022" */
    YEAR,

    /** year shortened, e.g. "18", "22" */
    YEAR_SHORT;

    companion object {
        fun formatString(format: DateFormat) = when(format) {
            TIME_OF_DAY -> "k:mm"
            DAY_MONTH_YEAR -> "dd.MM.yyyy"
            DAY_OF_MONTH_PADDED -> "dd"
            DAY_OF_MONTH -> "d"
            DAY_AND_MONTH -> "dd.MM."
            WEEKDAY_ABBREVIATED -> "E"
            WEEKDAY_SINGLE_LETTER -> "EEEEE"
            MONTH -> "MMMM"
            MONTH_ABBREVIATED -> "MMM"
            YEAR -> "y"
            YEAR_SHORT -> "yy"
        }
    }
}

fun ZonedDateTime.musikusFormat(format: DateFormat) : String =
    this.format(
        DateTimeFormatter.ofPattern(
            (DateFormat.formatString(format) + if (
                format == DateFormat.TIME_OF_DAY &&
                this.offset != ZonedDateTime.now().offset
            ) "z" else "")
        )
    )

fun ZonedDateTime.musikusFormat(formatList: List<DateFormat>) : String =
    formatList.joinToString(" ") { this.musikusFormat(it) }

fun Timeframe.musikusFormat() : String {
    val (start, end) = this

    val yearDifference = end.year - start.year
    val monthDifference = end.specificMonth - start.specificMonth

    val dateFormat =
        if (monthDifference > 3)
            if(yearDifference > 0) listOf(DateFormat.MONTH, DateFormat.YEAR_SHORT)
            else listOf(DateFormat.MONTH)
        else
            if(yearDifference > 0) listOf(DateFormat.DAY_MONTH_YEAR)
            else listOf(DateFormat.DAY_AND_MONTH)

    return start.musikusFormat(dateFormat) + " - " + end.musikusFormat(dateFormat)
}



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
 * @param format one of TimeFormat.XXX variables, indicating the output format
 * @return A CharSequence (either a String or a SpannableString) with the final String.
 */
fun getDurationString(durationSeconds: Int, format: TimeFormat, scale: Float = 0.6f): CharSequence {
    val (hours, minutes, seconds) = secondsDurationToHoursMinSec(durationSeconds)
    val days = secondsDurationToFullDays(durationSeconds)

    val spaceOrNot = when(format) {
        TimeFormat.HUMAN_PRETTY -> " "
        TimeFormat.HUMAN_PRETTY_SHORT -> ""
        else -> ""
    }

    when (format) {
        TimeFormat.HUMAN_PRETTY, TimeFormat.HUMAN_PRETTY_SHORT -> {
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

        TimeFormat.HMS_DIGITAL -> {
            return "%02d:%02d:%02d".format(
                hours,
                minutes,
                seconds
            )
        }

        TimeFormat.MS_DIGITAL -> {
            return "%02d:%02d".format(
                hours * 60 + minutes,
                seconds
            )
        }

        TimeFormat.HM_DIGITAL_OR_MIN_HUMAN -> {
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
        TimeFormat.PRETTY_APPROX -> {
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

        TimeFormat.PRETTY_APPROX_SHORT -> {
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

fun getTimestamp(
    dateTime: ZonedDateTime = ZonedDateTime.now()
) = dateTime.toEpochSecond()

//fun getCurrentDateTime(): ZonedDateTime = ZonedDateTime.now()

// copies the time from the original timezone to the local timezone without adjusting it
fun ZonedDateTime.inLocalTimezone(): ZonedDateTime =
    this.toLocalDateTime().atZone(ZonedDateTime.now().zone)

/**
 * Get the Beginning of dayOffset Days from now. dayOffset>0 -> future, dayOffset<0 -> past
 */
fun getStartOfDay(
    dayOffset: Long = 0,
    weekOffset: Long = 0,
    monthOffset: Long = 0,
    dateTime: ZonedDateTime = ZonedDateTime.now()
): ZonedDateTime = dateTime
    .with(ChronoField.MILLI_OF_DAY, 0)
    .toLocalDate()
    .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
    .plusMonths(monthOffset)
    .plusWeeks(weekOffset)
    .plusDays(dayOffset)

/**
 * Get the End of dayOffset Days from now. Half-open: Actually get the Start of the Next day
 */
fun getEndOfDay(
    dayOffset: Long = 0,
    weekOffset: Long = 0,
    monthOffset: Long = 0,
    dateTime: ZonedDateTime = ZonedDateTime.now()
) = getStartOfDay(dayOffset + 1, weekOffset, monthOffset, dateTime)

/**
 * get the Beginning of a Day (1=Mo, 7=Sun) of the current week (weekOffset=0) / the weeks before (weekOffset<0)
  */
fun getStartOfDayOfWeek(
    dayIndex: Long = 0,
    weekOffset: Long,
    dateTime: ZonedDateTime = ZonedDateTime.now()
): ZonedDateTime = dateTime
    .with(ChronoField.SECOND_OF_DAY, 0)
    .with(ChronoField.DAY_OF_WEEK , dayIndex )         // ISO 8601, Monday is first day of week.
    .toLocalDate()
    .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
    .plusWeeks(weekOffset)

/**
 * get the End of a Day (1=Mo, 7=Sun) of the current week (weekOffset=0) / the weeks before (weekOffset<0)
 */
fun getEndOfDayOfWeek(
    dayIndex: Long,
    weekOffset: Long,
    dateTime: ZonedDateTime = ZonedDateTime.now()
): ZonedDateTime {
    // because of half-open approach we have to get the "start of the _next_ day" instead of the end of the current day
    // e.g. end of Tuesday = Start of Wednesday, so make dayIndex 2 -> 3
    var nextDay = dayIndex + 1
    var weekOffsetAdapted = weekOffset
    if (dayIndex > 6) {
        nextDay = (dayIndex + 1) % 7
        weekOffsetAdapted += 1  // increase weekOffset so that we take the start of the first day of NEXT week as end of day
    }
    return getStartOfDayOfWeek(nextDay, weekOffsetAdapted, dateTime)
}

/**
 * gets start date of a Week
 */
fun getStartOfWeek(
    weekOffset: Long = 0,
    dateTime: ZonedDateTime = ZonedDateTime.now()
) = getStartOfDayOfWeek(1, weekOffset, dateTime)



/**
 * gets end date of a Week
  */
fun getEndOfWeek(
    weekOffset: Long = 0,
    dateTime: ZonedDateTime = ZonedDateTime.now()
) = getEndOfDayOfWeek(7, weekOffset, dateTime)

/**
 * returns the weekDay of today from index 1=Mo until 7=Sun
  */

fun getDayIndexOfWeek(
    dateTime: ZonedDateTime = ZonedDateTime.now()
) = dateTime.toLocalDate().dayOfWeek.value

/**
 * Get the start of a month.
 * monthOffset=0 ->, monthOffset=1 -> next month, monthOffset = -1 -> last month, etc.
 */
fun getStartOfMonth(
    monthOffset: Long = 0,
    dateTime: ZonedDateTime = ZonedDateTime.now()
): ZonedDateTime = dateTime
    .with(ChronoField.SECOND_OF_DAY, 0)
    .with(ChronoField.DAY_OF_MONTH , 1 )    // jump to first day of this month
    .toLocalDate()
    .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
    .plusMonths(monthOffset) // add desired number of months from now

/**
 * Get the end of a month. Half-open: Actually get the Start of the next month.
 */
fun getEndOfMonth(
    monthOffset: Long = 0,
    dateTime: ZonedDateTime = ZonedDateTime.now()
) = getStartOfMonth(monthOffset + 1, dateTime)

/**
 * Get specific month as in
 * January 2011 is different from January 2020
 */

val ZonedDateTime.specificMonth
    get() = this.monthValue + this.year * 12

/**
 * Get specific week index (Non reversible hash bucket)
 */

val ZonedDateTime.specificWeek
    get() = this
        .with(ChronoField.DAY_OF_WEEK , 1)         // ISO 8601, Monday is first day of week.
        .let { date->
            date.dayOfYear + date.year * 366
        }


/**
 * Get specificDay index (Non reversible hash bucket)
 */

val ZonedDateTime.specificDay
    get() = this.dayOfYear + this.year * 366

fun weekIndexToName(weekIndex: Int) = when (weekIndex) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    else -> "ERROR"
}