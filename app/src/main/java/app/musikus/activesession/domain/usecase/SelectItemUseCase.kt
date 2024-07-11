/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.activesession.domain.usecase


import app.musikus.library.data.daos.LibraryItem
import app.musikus.activesession.domain.ActiveSessionRepository
import app.musikus.activesession.domain.PracticeSection
import app.musikus.activesession.domain.SessionState
import app.musikus.core.domain.IdProvider
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.minus
import app.musikus.core.domain.plus
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

class SelectItemUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val resumeUseCase: ResumeActiveSessionUseCase,
    private val getRunningItemDurationUseCase: GetRunningItemDurationUseCase,
    private val timeProvider: TimeProvider,
    private val idProvider: IdProvider
) {
    suspend operator fun invoke(libraryItem: LibraryItem) {
        val state = activeSessionRepository.getSessionState().first()


        if (state != null) {  /** another section is already running */
            // check same item
            if (state.currentSectionItem == libraryItem) {
                throw IllegalStateException("Must not select the same library item which is already running.")
            }

            // check too fast
            if (getRunningItemDurationUseCase() < 1.seconds) {
                throw IllegalStateException("Must wait for at least one second before starting a new section.")
            }

            // resume if paused
            if (state.isPaused) resumeUseCase()


            // take time
            val runningSectionTrueDuration = getRunningItemDurationUseCase()
            val changeSectionTimestamp = state.startTimestampSectionPauseCompensated + runningSectionTrueDuration.inWholeSeconds.seconds
            // running section duration calculated until changeSectionTimestamp
            val runningSectionRoundedDuration = getRunningItemDurationUseCase(at = changeSectionTimestamp)

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
                    currentSectionItem = libraryItem,
                    startTimestampSection = changeSectionTimestamp, // new sections starts when the old one ends
                    startTimestampSectionPauseCompensated = changeSectionTimestamp,
                )
            )
            return
       }

        /** starting the first section */
        val changeOverTime = timeProvider.now()
        activeSessionRepository.setSessionState(
            SessionState( // create new session state
                completedSections = emptyList(),
                currentSectionItem = libraryItem,
                startTimestamp = changeOverTime,
                startTimestampSection = changeOverTime,
                startTimestampSectionPauseCompensated = changeOverTime,
                currentPauseStartTimestamp = null,
                isPaused = false
            )
        )
    }
}