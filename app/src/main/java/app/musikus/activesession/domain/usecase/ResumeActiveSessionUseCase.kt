/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.domain.ActiveSessionRepository
import app.musikus.core.domain.plus
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

class ResumeActiveSessionUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val computeOngoingPauseDurationUseCase: ComputeOngoingPauseDurationUseCase,
) {
    suspend operator fun invoke(at: ZonedDateTime) {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Cannot resume when state is null")

        if (!state.isPaused) {
            throw IllegalStateException("Cannot resume when not paused")
        }

        val currentPauseDuration = computeOngoingPauseDurationUseCase(state, at)
        activeSessionRepository.setSessionState(
            state.copy(
                startTimestampSectionPauseCompensated =
                state.startTimestampSectionPauseCompensated + currentPauseDuration,
                currentPauseStartTimestamp = null,
                isPaused = false
            )
        )
    }
}
