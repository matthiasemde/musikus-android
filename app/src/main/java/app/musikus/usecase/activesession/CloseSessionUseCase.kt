package app.musikus.usecase.activesession

import app.musikus.utils.IdProvider
import app.musikus.utils.minus
import app.musikus.utils.plus
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

class CloseSessionUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val getRunningSectionUseCase: GetRunningSectionUseCase,
    private val resumeUseCase: ResumeUseCase,
    private val idProvider: IdProvider
) {
    suspend operator fun invoke() {
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

        // update session state
        activeSessionRepository.setSessionState(
            state.copy(
                completedSections = updatedSections
            )
        )
    }

}