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
import kotlinx.coroutines.flow.map

enum class SessionTimerState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
}

class GetSessionTimerState (
    private val activeSessionRepository: ActiveSessionRepository,
) {
    operator fun invoke() : Flow<SessionTimerState> {

        return activeSessionRepository.getSessionState().map { state ->
            if (state == null) {
                return@map SessionTimerState.NOT_STARTED
            }
            if (state.isPaused)
                return@map SessionTimerState.PAUSED

            SessionTimerState.RUNNING
        }
    }
}