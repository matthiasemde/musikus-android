/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.domain.ActiveSessionRepository
import app.musikus.activesession.domain.PracticeSection
import app.musikus.activesession.domain.SessionState
import app.musikus.core.domain.IdProvider
import app.musikus.core.domain.minus
import app.musikus.core.domain.plus
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

class GetFinalizedSessionUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val computeRunningItemDuration: ComputeRunningItemDurationUseCase,
    private val computeOngoingPauseDuration: ComputeOngoingPauseDurationUseCase,
    private val idProvider: IdProvider
) {
    suspend operator fun invoke(at: ZonedDateTime): SessionState {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("State is null. Cannot finish session!")

        // take time
        val runningSectionRoundedDuration = computeRunningItemDuration(state, at).inWholeSeconds.seconds
        val ongoingPauseDuration = computeOngoingPauseDuration(state, at)

        // append finished section to completed sections
        val updatedSections = state.completedSections
            .plus(
                PracticeSection(
                    id = idProvider.generateId(),
                    libraryItem = state.currentSectionItem,
                    pauseDuration = (
                        state.startTimestampSectionPauseCompensated + ongoingPauseDuration
                        ) - state.startTimestampSection,
                    duration = runningSectionRoundedDuration,
                    startTimestamp = state.startTimestampSection
                )
            )
            .filter { it.duration > 0.seconds }

        if (updatedSections.isEmpty()) {
            throw IllegalStateException("Completed sections are empty.")
        }

        return state.copy(
            completedSections = updatedSections
        )
    }
}
