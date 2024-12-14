/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.domain.SessionState
import app.musikus.core.domain.minus
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ComputeOngoingPauseDurationUseCase {
    operator fun invoke(
        state: SessionState,
        at: ZonedDateTime
    ): Duration {
        if (state.currentPauseStartTimestamp == null) return 0.seconds
        val duration = at - state.currentPauseStartTimestamp
        if (duration < 0.seconds) {
            throw IllegalStateException("Duration is negative. This should not happen.")
        }
        return duration
    }
}
