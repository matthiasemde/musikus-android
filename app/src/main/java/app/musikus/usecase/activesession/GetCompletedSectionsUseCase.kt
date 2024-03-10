package app.musikus.usecase.activesession

import kotlinx.coroutines.flow.map

class GetCompletedSectionsUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    operator fun invoke() = activeSessionRepository.getSessionState().map {
        it?.completedSections ?: emptyList()
    }
}