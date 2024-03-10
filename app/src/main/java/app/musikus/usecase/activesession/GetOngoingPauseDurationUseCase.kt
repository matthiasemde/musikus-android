package app.musikus.usecase.activesession

import app.musikus.utils.TimeProvider
import app.musikus.utils.minus
import kotlinx.coroutines.flow.first
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GetOngoingPauseDurationUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke() : Duration {
        val state = activeSessionRepository.getSessionState().first() ?: return 0.seconds
        if (state.currentPauseStartTimestamp == null) {
            throw IllegalStateException("Pause Timestamp is null, cannot get ongoing pause duration.")
        }
        return timeProvider.now() - state.currentPauseStartTimestamp
    }
}