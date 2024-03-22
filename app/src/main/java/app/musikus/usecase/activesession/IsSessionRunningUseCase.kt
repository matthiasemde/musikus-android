package app.musikus.usecase.activesession

class IsSessionRunningUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    operator fun invoke() : Boolean {
        return activeSessionRepository.isRunning()
    }
}