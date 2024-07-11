/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.domain.ActiveSessionRepository
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.minus
import kotlinx.coroutines.flow.first
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GetOngoingPauseDurationUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() : Duration {
        val state = activeSessionRepository.getSessionState().first() ?: return 0.seconds
        if (state.currentPauseStartTimestamp == null) return 0.seconds
        val duration = timeProvider.now() - state.currentPauseStartTimestamp
        if (duration < 0.seconds) {
            throw IllegalStateException("Duration is negative. This should not happen.")
        }
        return duration
    }
}