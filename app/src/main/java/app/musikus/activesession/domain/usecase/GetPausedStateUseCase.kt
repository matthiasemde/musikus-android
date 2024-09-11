/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.domain.ActiveSessionRepository
import kotlinx.coroutines.flow.first

class GetPausedStateUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(): Boolean {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Cannot get paused state when state is null")
        return state.isPaused
    }
}
