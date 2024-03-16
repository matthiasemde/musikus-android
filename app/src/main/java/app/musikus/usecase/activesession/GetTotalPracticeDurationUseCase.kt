/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.usecase.activesession


import kotlinx.coroutines.flow.first
import kotlin.time.Duration

class GetTotalPracticeDurationUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val runningItemDurationUseCase: GetRunningItemDurationUseCase
) {
    suspend operator fun invoke() : Duration {
        val state = activeSessionRepository.getSessionState().first()
        val runningItemDuration = runningItemDurationUseCase()

        if (state == null) {
            throw IllegalStateException("State is null. Cannot get total practice time!")
        }

        // add up all completed section durations
        // add running section duration on top (by using initial value of fold)
        return state.completedSections.fold(runningItemDuration) { acc, section ->
            acc + section.duration
        }
    }
}