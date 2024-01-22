package app.musikus.usecase.sessions

import app.musikus.repository.SessionRepository
import java.util.UUID

class DeleteSessionsUseCase(
    private val sessionRepository: SessionRepository
) {

    suspend operator fun invoke(ids : List<UUID>) {
        sessionRepository.delete(ids)
    }
}