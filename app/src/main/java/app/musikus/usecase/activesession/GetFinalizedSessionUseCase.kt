/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.usecase.activesession

import app.musikus.utils.IdProvider
import app.musikus.utils.minus
import app.musikus.utils.plus
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

class GetFinalizedSessionUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val getRunningSectionUseCase: GetRunningSectionUseCase,
    private val resumeUseCase: ResumeActiveSessionUseCase,
    private val idProvider: IdProvider
) {
    suspend operator fun invoke(): SessionState {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("State is null. Cannot finish session!")

        // resume if paused to get correct duration
        if (state.isPaused) resumeUseCase()

        // take time
        val runningSectionTrueDuration = getRunningSectionUseCase().second
        val changeOverTime = state.startTimestampSectionPauseCompensated + runningSectionTrueDuration.inWholeSeconds.seconds
        val runningSectionRoundedDuration = getRunningSectionUseCase(at = changeOverTime).second

        // append finished section to completed sections
        val updatedSections = state.completedSections + PracticeSection(
            id = idProvider.generateId(),
            libraryItem = state.currentSectionItem,
            pauseDuration = state.startTimestampSectionPauseCompensated - state.startTimestampSection,
            duration = runningSectionRoundedDuration
        )

        return state.copy(
            completedSections = updatedSections
        )
    }

}