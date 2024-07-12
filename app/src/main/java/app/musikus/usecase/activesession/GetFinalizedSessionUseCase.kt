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