/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Good comment from Jon Skeet on this topic: https://stackoverflow.com/a/5622222/20420131

interface TimeProvider {
    fun now(): ZonedDateTime

    fun localZoneId(): ZoneId = now().zone

    /**
     * Get the Beginning of dayOffset Days from now. dayOffset>0 -> future, dayOffset<0 -> past
     */
    fun getStartOfDay(
        dayOffset: Long = 0,
        weekOffset: Long = 0,
        monthOffset: Long = 0,
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime = dateTime
        .with(ChronoField.MILLI_OF_DAY, 0)
        .toLocalDate()
        .atStartOfDay(dateTime.zone)  // make sure time is 00:00
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
        dateTime: ZonedDateTime = now()
    ) = getStartOfDay(dayOffset + 1, weekOffset, monthOffset, dateTime)

    /**
     * get the Beginning of a Day (1=Mo, 7=Sun) of the current week (weekOffset=0) / the weeks before (weekOffset<0)
     */
    fun getStartOfDayOfWeek(
        dayIndex: Long = 0,
        weekOffset: Long,
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime = dateTime
        .with(ChronoField.SECOND_OF_DAY, 0)
        .with(ChronoField.DAY_OF_WEEK , dayIndex )         // ISO 8601, Monday is first day of week.
        .toLocalDate()
        .atStartOfDay(dateTime.zone)  // make sure time is 00:00
        .plusWeeks(weekOffset)

    /**
     * get the End of a Day (1=Mo, 7=Sun) of the current week (weekOffset=0) / the weeks before (weekOffset<0)
     */
    fun getEndOfDayOfWeek(
        dayIndex: Long,
        weekOffset: Long,
        dateTime: ZonedDateTime = now()
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
        dateTime: ZonedDateTime = now()
    ) = getStartOfDayOfWeek(1, weekOffset, dateTime)



    /**
     * gets end date of a Week
     */
    fun getEndOfWeek(
        weekOffset: Long = 0,
        dateTime: ZonedDateTime = now()
    ) = getEndOfDayOfWeek(7, weekOffset, dateTime)


    /**
     * Get the start of a month.
     * monthOffset=0 ->, monthOffset=1 -> next month, monthOffset = -1 -> last month, etc.
     */
    fun getStartOfMonth(
        monthOffset: Long = 0,
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime = dateTime
        .with(ChronoField.SECOND_OF_DAY, 0)
        .with(ChronoField.DAY_OF_MONTH , 1 )    // jump to first day of this month
        .toLocalDate()
        .atStartOfDay(dateTime.zone)  // make sure time is 00:00
        .plusMonths(monthOffset) // add desired number of months from now

    /**
     * Get the end of a month. Half-open: Actually get the Start of the next month.
     */
    fun getEndOfMonth(
        monthOffset: Long = 0,
        dateTime: ZonedDateTime = now()
    ) = getStartOfMonth(monthOffset + 1, dateTime)

    companion object {
        val uninitializedDateTime: ZonedDateTime =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.systemDefault())
    }
}

class TimeProviderImpl : TimeProvider {

    override fun now(): ZonedDateTime {
        return ZonedDateTime.now()
    }
}

/**
 * Operator extension function to get the duration between two ZonedDateTime objects.
 * @param other the earlier ZonedDateTime
 */
operator fun ZonedDateTime.minus(other: ZonedDateTime): Duration {
    return ChronoUnit.MILLIS.between(other, this).milliseconds
}

/**
 * Operator extension function to get a ZonedDateTime object with a duration added..
 * @param duration the duration to add.
 */
operator fun ZonedDateTime.plus(duration: Duration): ZonedDateTime {
    return this.plusNanos(duration.inWholeNanoseconds)
}

/**
 * Operator extension function to get a ZonedDateTime object with a duration subtracted.
 * @param duration the duration to subtract
 */
operator fun ZonedDateTime.minus(duration: Duration): ZonedDateTime {
    return this.minusNanos(duration.inWholeNanoseconds)
}