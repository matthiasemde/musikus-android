package app.musikus.usecase.activesession

class ResetSessionUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    operator fun invoke() {
        activeSessionRepository.reset()
    }
}