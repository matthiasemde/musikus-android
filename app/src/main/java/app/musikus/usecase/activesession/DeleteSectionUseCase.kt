package app.musikus.usecase.activesession


import kotlinx.coroutines.flow.first

class DeleteSectionUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(practiceSection: PracticeSection) {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Sections cannot be deleted when state is null")

        activeSessionRepository.setSessionState(
            state.copy(
                completedSections = state.completedSections.filter { it.id != practiceSection.id }
            )
        )
    }
}