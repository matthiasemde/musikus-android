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
import app.musikus.library.data.daos.LibraryItem
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

class SelectItemUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val getRunningItemDuration: GetRunningItemDurationUseCase,
    private val idProvider: IdProvider
) {
    suspend operator fun invoke(
        item: LibraryItem,
        at: ZonedDateTime,
    ) {
        val state = activeSessionRepository.getSessionState().first()

        if (state != null) {
            /** another section is already running */
            // check same item
            if (state.currentSectionItem == item) {
                throw IllegalStateException("Must not select the same library item which is already running.")
            }

            // check too fast
            if (getRunningItemDuration(at) < 1.seconds) {
                throw IllegalStateException("Must wait for at least one second before starting a new section.")
            }

            // only start new item when not paused
            if (state.isPaused) throw IllegalStateException("You must resume before selecting a new item.")

            // take time
            val runningSectionTrueDuration = getRunningItemDuration(at)
            val changeSectionTimestamp = state.startTimestampSectionPauseCompensated + runningSectionTrueDuration.inWholeSeconds.seconds
            // running section duration calculated until changeSectionTimestamp
            val runningSectionRoundedDuration = getRunningItemDuration(at = changeSectionTimestamp)

            // append finished section to completed sections
            val updatedSections = state.completedSections + PracticeSection(
                id = idProvider.generateId(),
                libraryItem = state.currentSectionItem,
                pauseDuration = state.startTimestampSectionPauseCompensated - state.startTimestampSection,
                duration = runningSectionRoundedDuration,
                startTimestamp = state.startTimestampSection
            )
            // adjust session state
            activeSessionRepository.setSessionState(
                state.copy(
                    completedSections = updatedSections,
                    currentSectionItem = item,
                    startTimestampSection = changeSectionTimestamp, // new sections starts when the old one ends
                    startTimestampSectionPauseCompensated = changeSectionTimestamp,
                )
            )
            return
        }

        /** starting the first section */
        val changeOverTime = at
        activeSessionRepository.setSessionState(
            SessionState( // create new session state
                completedSections = emptyList(),
                currentSectionItem = item,
                startTimestamp = changeOverTime,
                startTimestampSection = changeOverTime,
                startTimestampSectionPauseCompensated = changeOverTime,
                currentPauseStartTimestamp = null,
                isPaused = false
            )
        )
    }
}
