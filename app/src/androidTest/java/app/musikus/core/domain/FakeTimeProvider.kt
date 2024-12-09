/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class FakeTimeProvider : TimeProvider {
    private val _clock = MutableStateFlow(START_TIME)
    override val clock: Flow<ZonedDateTime> get() = _clock.asStateFlow()

    override fun now(): ZonedDateTime {
        return _clock.value
    }

    fun setCurrentDateTime(dateTime: ZonedDateTime) {
        _clock.update { dateTime }
    }

    fun advanceTimeBy(duration: Duration) {
        _clock.update { it.plus(duration.toJavaDuration()) }
    }

    fun revertTimeBy(duration: Duration) {
        _clock.update { it.minus(duration.toJavaDuration()) }
    }

    companion object {
        val START_TIME: ZonedDateTime = ZonedDateTime.parse("1969-07-20T20:17:40Z[UTC]")
    }
}
