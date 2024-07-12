/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.data.ActiveSessionRepository
import app.musikus.core.domain.TimeProvider
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

class PauseActiveSessionUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val getRunningItemDurationUseCase: GetRunningItemDurationUseCase,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Cannot pause when state is null")

        if (state.isPaused) {
            throw IllegalStateException("Cannot pause when already paused.")
        }

        // ignore pause if first section is running and less than 1 second has passed
        // (prevents finishing empty session)
        if (state.completedSections.isEmpty() && getRunningItemDurationUseCase() < 1.seconds) return

        activeSessionRepository.setSessionState(
            state.copy(
                currentPauseStartTimestamp = timeProvider.now(),
                isPaused = true
            )
        )
    }
}