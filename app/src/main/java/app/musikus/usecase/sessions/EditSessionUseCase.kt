package app.musikus.usecase.sessions

import app.musikus.database.daos.InvalidSectionException
import app.musikus.database.daos.InvalidSessionException
import app.musikus.database.entities.SectionUpdateAttributes
import app.musikus.database.entities.SessionUpdateAttributes
import app.musikus.repository.SessionRepository
import java.util.UUID

class EditSessionUseCase(
    private val sessionRepository: SessionRepository
) {

    suspend operator fun invoke(
        id: UUID,
        sessionUpdateAttributes: SessionUpdateAttributes,
        sectionUpdateData: List<Pair<UUID, SectionUpdateAttributes>>
    ) {

        if (!sessionRepository.existsSession(id)) {
            throw IllegalArgumentException("Session with id $id does not exist")
        }

        if (sessionUpdateAttributes.rating != null && sessionUpdateAttributes.rating !in 1..5) {
            throw InvalidSessionException("Rating must be between 1 and 5")
        }

        if (sessionUpdateAttributes.comment != null && sessionUpdateAttributes.comment.length > 500) {
            throw InvalidSessionException("Comment must be less than 500 characters")
        }

        val sectionIdsForSession = sessionRepository.sectionsForSession(id).map { it.id }

        val sectionIdsNotInSession = sectionUpdateData.unzip().first.filter { sectionId -> sectionId !in sectionIdsForSession }
        if (sectionIdsNotInSession.isNotEmpty()) {
            throw IllegalArgumentException("Section(s) with id(s) $sectionIdsNotInSession are not in session with id $id")
        }

        if (sectionUpdateData.unzip().second.any { it.duration != null && it.duration.inWholeSeconds <= 0 }) {
            throw InvalidSectionException("Section duration must be greater than 0")
        }

        sessionRepository.withTransaction {
            sessionRepository.updateSession(id, sessionUpdateAttributes)

            sessionRepository.updateSections(
                sectionUpdateData
            )
        }
    }
}