package app.musikus.usecase.activesession

import app.musikus.utils.TimeProvider
import app.musikus.utils.plus
import kotlinx.coroutines.flow.first

class ResumeUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val getOngoingPauseDurationUseCase: GetOngoingPauseDurationUseCase,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Cannot resume when state is null")

        if (!state.isPaused) {
            throw IllegalStateException("Cannot resume when not paused")
        }

        val currentPauseDuration = getOngoingPauseDurationUseCase()
        activeSessionRepository.setSessionState(
            state.copy(
                startTimestampSectionPauseCompensated =
                    state.startTimestampSectionPauseCompensated + currentPauseDuration,
                isPaused = false
            )
        )
    }
}