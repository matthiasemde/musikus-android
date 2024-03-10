package app.musikus.usecase.activesession

import app.musikus.database.daos.LibraryItem
import app.musikus.utils.IdProvider
import app.musikus.utils.TimeProvider
import app.musikus.utils.minus
import app.musikus.utils.plus
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

class SelectItemUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val resumeUseCase: ResumeUseCase,
    private val getRunningSectionUseCase: GetRunningSectionUseCase,
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
            if (getRunningSectionUseCase().second < 1.seconds) {
                throw IllegalStateException("Must wait for at least one second before starting a new section.")
            }

            // resume if paused
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
            // adjust session state
            activeSessionRepository.setSessionState(
                state.copy(
                    completedSections = updatedSections,
                    currentSectionItem = libraryItem,
                    startTimestampSection = changeOverTime,
                    startTimestampSectionPauseCompensated = changeOverTime,
                )
            )
            return
       }

        /** starting the first section */
        val changeOverTime = timeProvider.now()
        SessionState( // create new session state
            completedSections = emptyList(),
            currentSectionItem = libraryItem,
            startTimestamp = changeOverTime,
            startTimestampSection = changeOverTime,
            startTimestampSectionPauseCompensated = changeOverTime,
            currentPauseStartTimestamp = null,
            isPaused = false
        )
    }
}