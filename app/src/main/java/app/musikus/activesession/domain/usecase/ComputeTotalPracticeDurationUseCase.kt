/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.domain.SessionState
import java.time.ZonedDateTime
import kotlin.time.Duration

class ComputeTotalPracticeDurationUseCase(
    private val computeRunningItemDuration: ComputeRunningItemDurationUseCase
) {
    operator fun invoke(
        state: SessionState,
        at: ZonedDateTime
    ): Duration {
        val runningItemDuration = computeRunningItemDuration(state, at)

        // add up all completed section durations
        // add running section duration on top (by using initial value of fold)
        return state.completedSections.fold(runningItemDuration) { acc, section ->
            acc + section.duration
        }
    }
}
