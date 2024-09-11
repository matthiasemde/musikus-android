/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.domain.ActiveSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SessionStatus {
    NOT_STARTED,
    RUNNING,
    PAUSED
}

class GetSessionStatusUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
) {
    operator fun invoke(): Flow<SessionStatus> {
        return activeSessionRepository.getSessionState().map { state ->
            if (state == null) {
                return@map SessionStatus.NOT_STARTED
            }
            if (state.isPaused) {
                return@map SessionStatus.PAUSED
            }

            SessionStatus.RUNNING
        }
    }
}
