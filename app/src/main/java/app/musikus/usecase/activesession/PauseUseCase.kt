package app.musikus.usecase.activesession

import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

class PauseUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val getRunningSectionUseCase: GetRunningSectionUseCase,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Cannot pause when state is null")

        if (state.isPaused) return

        // ignore pause if first section is running and less than 1 second has passed
        // (prevents finishing empty session)
        if (state.completedSections.isEmpty() && getRunningSectionUseCase().second < 1.seconds) return

        activeSessionRepository.setSessionState(
            state.copy(
                currentPauseStartTimestamp = timeProvider.now(),
                isPaused = true
            )
        )
    }
}