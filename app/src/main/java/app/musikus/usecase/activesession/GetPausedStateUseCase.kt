package app.musikus.usecase.activesession


import kotlinx.coroutines.flow.first

class GetPausedStateUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke() : Boolean {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Cannot get paused state when state is null")
        return state.isPaused
    }
}