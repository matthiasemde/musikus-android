package app.musikus.usecase.sessions

import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.repository.SessionRepository
import app.musikus.utils.Timeframe
import kotlinx.coroutines.flow.Flow

class GetSessionsInTimeframeUseCase(
    private val sessionRepository: SessionRepository
) {

    operator fun invoke(timeframe: Timeframe): Flow<List<SessionWithSectionsWithLibraryItems>> {
        return sessionRepository.sessionsInTimeframe(timeframe = timeframe)
    }
}