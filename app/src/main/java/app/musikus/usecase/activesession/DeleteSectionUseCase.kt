package app.musikus.usecase.activesession


import kotlinx.coroutines.flow.first
import java.util.UUID

class DeleteSectionUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    suspend operator fun invoke(sectionId: UUID) {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("Sections cannot be deleted when state is null")

        activeSessionRepository.setSessionState(
            state.copy(
                completedSections = state.completedSections.filter { it.id != sectionId }
            )
        )
    }
}