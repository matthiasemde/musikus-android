/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.utils

import app.musikus.core.domain.minus
import com.google.common.truth.Truth.assertThat
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class TimeProviderTest {

    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var timeProviderSpy: FakeTimeProvider

    @BeforeEach
    fun setUp() {
        timeProvider = FakeTimeProvider()
        timeProviderSpy = spyk(timeProvider)
    }

    @Test
    fun `Subtract two timestamps in the same timezone`() {
        val start = ZonedDateTime.parse("2021-01-01T00:00:00.000+01:00[Europe/Berlin]")
        val end = ZonedDateTime.parse("2021-01-01T00:00:01.000+01:00[Europe/Berlin]")
        val duration = end - start
        assertThat(duration).isEqualTo(1000.milliseconds)
    }

    @Test
    fun `Subtract two timestamps in different timezones`() {
        val start = ZonedDateTime.parse("2021-01-01T00:00:00.000+01:00[Europe/Berlin]")
        val end = ZonedDateTime.parse("2021-01-01T00:00:00.000+02:00[Europe/Berlin]")
        val duration = end - start
        assertThat(duration).isEqualTo((-1).hours)
    }

    @Test
    fun `Test sub-second precision`() {
        val start = ZonedDateTime.parse("2021-01-01T00:00:00.000+01:00[Europe/Berlin]")
        val end = ZonedDateTime.parse("2021-01-01T00:00:00.050+01:00[Europe/Berlin]")
        val duration = end - start
        assertThat(duration).isEqualTo(50.milliseconds)
    }


    /** -------------- Get start/end of day -------------------- */
    @Test
    fun `Get start of day`() {
        val startOfDay = timeProvider.getStartOfDay()
        assertThat(startOfDay).isEqualTo(ZonedDateTime.parse("1969-07-20T00:00:00Z[UTC]"))
    }

    @Test
    fun `Get start of day for specific datetime`() {
        val startOfDay = timeProvider.getStartOfDay(
            dateTime = ZonedDateTime.parse("1968-12-24T08:59:52Z[UTC]")
        )
        assertThat(startOfDay).isEqualTo(ZonedDateTime.parse("1968-12-24T00:00:00Z[UTC]"))
    }

    @Test
    fun `Get end of day`() {
        val endOfDay = timeProvider.getEndOfDay()
        assertThat(endOfDay).isEqualTo(ZonedDateTime.parse("1969-07-21T00:00:00Z[UTC]"))
    }

    @Test
    fun `Get end of day for specific datetime`() {
        val endOfDay = timeProvider.getEndOfDay(
            dateTime = ZonedDateTime.parse("1968-12-24T08:59:52Z[UTC]")
        )
        assertThat(endOfDay).isEqualTo(ZonedDateTime.parse("1968-12-25T00:00:00Z[UTC]"))
    }


    /** ----------- Get start/end of day of week ---------------- */

    @Test
    fun `Get start of day of week for monday`() {
        val startOfDayOfWeek = timeProvider.getStartOfDayOfWeek(1)
        assertThat(startOfDayOfWeek).isEqualTo(ZonedDateTime.parse("1969-07-14T00:00:00Z[UTC]"))
    }

    @Test
    fun `Get start of day of week for sunday`() {
        val startOfDayOfWeek = timeProvider.getStartOfDayOfWeek(7)
        assertThat(startOfDayOfWeek).isEqualTo(ZonedDateTime.parse("1969-07-20T00:00:00Z[UTC]"))
    }

    @Test
    fun `Get start of day of week for day index 8, throws assertion error`() {
        assertThrows<AssertionError> {
            timeProvider.getStartOfDayOfWeek(8)
        }
    }

    @Test
    fun `Get end of day of week for monday`() {
        val endOfDayOfWeek = timeProvider.getEndOfDayOfWeek(
            7,
            ZonedDateTime.parse("1961-05-05T09:50:00Z[UTC]")
        )
        assertThat(endOfDayOfWeek).isEqualTo(ZonedDateTime.parse("1961-05-08T00:00:00Z[UTC]"))
    }


    /** -------------- Get start/end of week -------------------- */
    @Test
    fun `Get start of week`() {
        val dateTime = ZonedDateTime.parse("1961-05-05T09:50:00Z[UTC]")

        timeProviderSpy.getStartOfWeek(dateTime = dateTime)

        verify (exactly = 1) { timeProviderSpy.getStartOfDayOfWeek(1, dateTime) }
    }

    @Test
    fun `Get end of week for specific datetime`() {
        val endOfWeek = timeProvider.getEndOfWeek(
            dateTime = ZonedDateTime.parse("1961-07-21T08:12:00Z[UTC]")
        )
        assertThat(endOfWeek).isEqualTo(ZonedDateTime.parse("1961-07-24T00:00:00Z[UTC]"))
    }


    /** -------------- Get start/end of month ------------------- */
    @Test
    fun `Get start of month`() {
        val startOfMonth = timeProvider.getStartOfMonth()
        assertThat(startOfMonth).isEqualTo(ZonedDateTime.parse("1969-07-01T00:00:00Z[UTC]"))
    }

    @Test
    fun `Get start of month for specific datetime`() {
        val startOfMonth = timeProvider.getStartOfMonth(
            dateTime = ZonedDateTime.parse("1968-12-24T08:59:52Z[UTC]")
        )
        assertThat(startOfMonth).isEqualTo(ZonedDateTime.parse("1968-12-01T00:00:00Z[UTC]"))
    }

    @Test
    fun `Get end of month`() {
        val endOfMonth = timeProvider.getEndOfMonth()
        assertThat(endOfMonth).isEqualTo(ZonedDateTime.parse("1969-08-01T00:00:00Z[UTC]"))
    }

    @Test
    fun `Get end of month for specific datetime`() {
        val endOfMonth = timeProvider.getEndOfMonth(
            dateTime = ZonedDateTime.parse("1968-12-24T08:59:52Z[UTC]")
        )
        assertThat(endOfMonth).isEqualTo(ZonedDateTime.parse("1969-01-01T00:00:00Z[UTC]"))
    }
}