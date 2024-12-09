/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
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

    val clock: Flow<ZonedDateTime>

    fun localZoneId(): ZoneId = now().zone

    /**
     * Get the beginning of current day.
     */
    fun getStartOfDay(
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime = dateTime.with(ChronoField.NANO_OF_DAY, 0)

    /**
     * Get the end of day. Half-open: Actually get the start of the next day.
     */
    fun getEndOfDay(
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime = getStartOfDay(dateTime).plusDays(1)

    /**
     * Get the beginning of a day (1=Mo, 7=Sun) of the current week (datetime).
     */
    fun getStartOfDayOfWeek(
        dayIndex: Long,
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime {
        assert(dayIndex in 1..7)

        return dateTime
            .with(ChronoField.NANO_OF_DAY, 0)
            .with(ChronoField.DAY_OF_WEEK, dayIndex) // ISO 8601, Monday is first day of week.
    }

    /**
     * Get the end of a day (1=Mo, 7=Sun) of the current week (datetime).
     * Half-open: Actually get the start of the next day.
     */
    fun getEndOfDayOfWeek(
        dayIndex: Long,
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime {
        assert(dayIndex in 1..7)

        return getStartOfDayOfWeek(dayIndex, dateTime).plusDays(1)
    }

    /**
     * Get start date of a week
     */
    fun getStartOfWeek(
        dateTime: ZonedDateTime = now()
    ) = getStartOfDayOfWeek(1, dateTime)

    /**
     * Get end date of a week. Half-open: Actually get the start of the next week.
     */
    fun getEndOfWeek(
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime = getStartOfWeek(dateTime).plusWeeks(1)

    /**
     * Get the start of a month.
     */
    fun getStartOfMonth(
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime = dateTime
        .with(ChronoField.NANO_OF_DAY, 0)
        .with(ChronoField.DAY_OF_MONTH, 1) // jump to first day of this month

    /**
     * Get the end of a month. Half-open: Actually get the start of the next month.
     */
    fun getEndOfMonth(
        dateTime: ZonedDateTime = now()
    ): ZonedDateTime = getStartOfMonth(dateTime).plusMonths(1)

    companion object {
        val uninitializedDateTime: ZonedDateTime =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.systemDefault())
    }
}

class TimeProviderImpl(scope: CoroutineScope) : TimeProvider {
    override val clock: StateFlow<ZonedDateTime> = flow {
        while (true) {
            emit(now()) // Emit the current time
            delay(100.milliseconds) // Wait 100 milliseconds
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = now()
    )

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
