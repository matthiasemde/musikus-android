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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

typealias Timeframe = Pair<ZonedDateTime, ZonedDateTime>

enum class DurationFormat {

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


/**
 * Get the formatted string for a duration of seconds, e.g. "1h 32m"
 *
 * The 'h' and 'm' are shrunk, if used. In that case,
 * the return value is a SpannableString which can be used directly in e.g. TextViews.
 * If you want to concat the returned CharSequence with another String, do it with
 * TextUtils.concat() to preserve styling!
 * If you NEED a String, simply call .toString().
 *
 * @param duration the duration in seconds to be converted
 * @param format one of TimeFormat.XXX variables, indicating the output format
 * @return A CharSequence (either a String or a SpannableString) with the final String.
 */
fun getDurationString(duration: Duration, format: DurationFormat, scale: Float = 0.6f): CharSequence {
    val days = duration.inWholeDays
    var remainingDuration = duration - days.days

    val hours = (remainingDuration).inWholeHours
    val totalHours = days * 24 + hours // total hours, including days
    remainingDuration -= hours.hours

    val minutes = (remainingDuration).inWholeMinutes
    remainingDuration -= minutes.minutes

    val seconds = (remainingDuration).inWholeSeconds

    val spaceOrNot = when(format) {
        DurationFormat.HUMAN_PRETTY -> " "
        DurationFormat.HUMAN_PRETTY_SHORT -> ""
        else -> ""
    }

    when (format) {
        DurationFormat.HUMAN_PRETTY, DurationFormat.HUMAN_PRETTY_SHORT -> {
            val str =
                if (totalHours > 0 && minutes > 0) {
                    ("%dh$spaceOrNot%02dm").format(totalHours, minutes)
                } else if (totalHours > 0L && minutes == 0L) {
                    "%dh".format(totalHours)
                } else if (minutes == 0L && duration.inWholeSeconds > 0L) {
                    "<" + spaceOrNot + "1m"
                } else {
                    ("%dm").format(minutes)
                }
            return getSpannableHourMinShrunk(str, scale)
        }

        DurationFormat.HMS_DIGITAL -> {
            return "%02d:%02d:%02d".format(
                totalHours,
                minutes,
                seconds
            )
        }

        DurationFormat.MS_DIGITAL -> {
            return "%02d:%02d".format(
                totalHours * 60 + minutes,
                seconds
            )
        }

        DurationFormat.HM_DIGITAL_OR_MIN_HUMAN -> {
            return when {
                totalHours > 0 -> {
                    "%02d:%02d".format(totalHours, minutes)
                }
                minutes > 0 -> {
                    "%d min".format(minutes)
                }
                else -> {
                    "< 1min"
                }
            }
        }
        DurationFormat.PRETTY_APPROX -> {
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

        DurationFormat.PRETTY_APPROX_SHORT -> {
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

/**
 * Copies the time from the original timezone to the local timezone without adjusting it
  */
fun ZonedDateTime.inLocalTimezone(timeProvider: TimeProvider): ZonedDateTime =
    this.toLocalDateTime().atZone(timeProvider.localZoneId())

/**
 * returns the weekDay of today from index 1=Mo until 7=Sun
  */

fun getDayIndexOfWeek(
    dateTime: ZonedDateTime
) = dateTime.toLocalDate().dayOfWeek.value

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