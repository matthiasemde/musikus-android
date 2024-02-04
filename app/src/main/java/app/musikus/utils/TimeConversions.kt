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

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

typealias Timeframe = Pair<ZonedDateTime, ZonedDateTime>

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
    YEAR_SHORT,

    /** date format used for naming recordings, e.g. "02_12_2024T20_21_22 e*/
    RECORDING;

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
            RECORDING -> "dd_MM_yyyy'T'H_mm_ss"
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