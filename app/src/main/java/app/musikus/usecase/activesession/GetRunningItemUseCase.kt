package app.musikus.usecase.activesession

import app.musikus.database.daos.LibraryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetRunningItemUseCase (
    private val activeSessionRepository: ActiveSessionRepository,
) {
    operator fun invoke() : Flow<LibraryItem?> {
        return activeSessionRepository.getSessionState().map {
            it?.currentSectionItem
        }
    }
}