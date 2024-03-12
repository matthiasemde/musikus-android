/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.usecase.activesession

import app.musikus.utils.plus
import kotlinx.coroutines.flow.first

class ResumeUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val getOngoingPauseDurationUseCase: GetOngoingPauseDurationUseCase,
) {
    suspend operator fun invoke() {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Cannot resume when state is null")

        if (!state.isPaused) {
            throw IllegalStateException("Cannot resume when not paused")
        }

        val currentPauseDuration = getOngoingPauseDurationUseCase()
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