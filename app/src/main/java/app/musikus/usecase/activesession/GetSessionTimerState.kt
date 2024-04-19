/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.usecase.activesession

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

enum class SessionTimerState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    FINISHED
}

class GetSessionTimerState (
    private val activeSessionRepository: ActiveSessionRepository,
) {
    operator fun invoke() : Flow<SessionTimerState> {
        if (!activeSessionRepository.isRunning()) return flow { SessionTimerState.NOT_STARTED }

        return activeSessionRepository.getSessionState().map { state ->
            if (state == null) {
                throw IllegalStateException("State is null although session is running.")
            }
            if (state.isPaused)
                return@map SessionTimerState.PAUSED

            SessionTimerState.RUNNING
        }
    }
}