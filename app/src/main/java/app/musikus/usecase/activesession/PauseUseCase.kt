package app.musikus.usecase.activesession

import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.first

class PauseUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Cannot pause when state is null")

        activeSessionRepository.setSessionState(
            state.copy(
                currentPauseStartTimestamp = timeProvider.now(),
                isPaused = true
            )
        )
    }
}