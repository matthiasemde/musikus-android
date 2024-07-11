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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class TimeProviderTest {


    @BeforeEach
    fun setUp() {

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
}