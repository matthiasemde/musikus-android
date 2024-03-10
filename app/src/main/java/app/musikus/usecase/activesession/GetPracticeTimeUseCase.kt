package app.musikus.usecase.activesession


import kotlinx.coroutines.flow.first
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GetPracticeTimeUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val runningSectionUseCase: GetRunningSectionUseCase
) {
    suspend operator fun invoke() : Duration {
        val state = activeSessionRepository.getSessionState().first()
        val runningSection = runningSectionUseCase()

        if (state == null) return 0.seconds

        // add up all completed section durations
        // add running section duration on top (by using initial value of fold)
        return state.completedSections.fold(runningSection.second) { acc, section ->
            acc + section.duration
        }
    }
}