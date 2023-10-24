package app.musikus.repository

import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.PTDatabase
import app.musikus.database.daos.Session
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.database.entities.SessionModel
import java.util.UUID

class SessionRepository(
    database: PTDatabase
) {
    private val sessionDao = database.sessionDao
    private val sectionDao = database.sectionDao


    /** Accessors */
    val sessions = sessionDao.getAllAsFlow()
    val sections = sectionDao.getAllAsFlow()

    val sessionsWithSectionsWithLibraryItems = sessionDao.getAllWithSectionsWithLibraryItemsAsFlow()
    fun sessionWithSectionsWithLibraryItems(id: UUID) = sessionDao.getWithSectionsWithLibraryItemsAsFlow(id)

    fun sectionsForGoal (goal: GoalInstanceWithDescriptionWithLibraryItems) =
        when(goal.description.description.type) {
            GoalType.ITEM_SPECIFIC -> sectionDao.get(
                startTimeStamp = goal.instance.startTimestamp,
                endTimeStamp = goal.instance.startTimestamp + goal.instance.periodInSeconds,
                itemIds = goal.description.libraryItems.map { it.id }
            )
            GoalType.NON_SPECIFIC -> sectionDao.get(
                startTimeStamp = goal.instance.startTimestamp,
                endTimeStamp = goal.instance.startTimestamp + goal.instance.periodInSeconds,
            )
        }

    /** Mutators */
    /** Add */
    suspend fun add(
        session: SessionCreationAttributes,
        sections: List<SectionCreationAttributes>
    ) : UUID {
        val newSession = SessionModel(
            breakDuration = session.breakDuration,
            rating = session.rating,
            comment = session.comment,
        )
        sessionDao.insert(newSession, sections)
        return newSession.id
    }

    /** Delete / Restore */
    suspend fun delete(sessions: List<Session>) {
        sessionDao.delete(sessions.map { it.id })
    }

    suspend fun restore(sessions: List<Session>) {
        sessionDao.restore(sessions.map { it.id })
    }

    /** Clean */
    suspend fun clean() {
        sessionDao.clean()
    }
}