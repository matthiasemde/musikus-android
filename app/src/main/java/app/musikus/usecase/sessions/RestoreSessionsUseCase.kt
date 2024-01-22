package app.musikus.usecase.sessions

import app.musikus.repository.SessionRepository
import java.util.UUID

class RestoreSessionsUseCase(
    private val sessionRepository: SessionRepository
) {

    suspend operator fun invoke(ids : List<UUID>) {
        sessionRepository.restore(ids)
    }
}