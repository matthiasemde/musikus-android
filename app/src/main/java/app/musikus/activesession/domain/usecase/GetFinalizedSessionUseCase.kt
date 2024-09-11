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
import kotlin.time.Duration.Companion.seconds

class GetFinalizedSessionUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val getRunningItemDurationUseCase: GetRunningItemDurationUseCase,
    private val getOngoingPauseDurationUseCase: GetOngoingPauseDurationUseCase,
    private val idProvider: IdProvider
) {
    suspend operator fun invoke(): SessionState {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("State is null. Cannot finish session!")

        // take time
        val runningSectionRoundedDuration = getRunningItemDurationUseCase().inWholeSeconds.seconds
        val ongoingPauseDuration = getOngoingPauseDurationUseCase()

        // append finished section to completed sections
        val updatedSections = state.completedSections + PracticeSection(
            id = idProvider.generateId(),
            libraryItem = state.currentSectionItem,
            pauseDuration = (state.startTimestampSectionPauseCompensated + ongoingPauseDuration) - state.startTimestampSection,
            duration = runningSectionRoundedDuration,
            startTimestamp = state.startTimestampSection
        )

        return state.copy(
            completedSections = updatedSections
        )
    }
}
