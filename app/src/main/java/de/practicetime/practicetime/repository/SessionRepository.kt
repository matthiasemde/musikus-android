package de.practicetime.practicetime.repository

import de.practicetime.practicetime.database.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.SessionWithSections
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.database.entities.Session

class SessionRepository(
    database: PTDatabase
) {
    private val sessionDao = database.sessionDao
    private val sectionDao = database.sectionDao


    /** Accessors */
    val sessions = sessionDao.getAllAsFlow()
    val sections = sectionDao.getAllAsFlow()

    val sessionsWithSectionsWithLibraryItems = sessionDao.getAllWithSectionsWithLibraryItems()

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
    suspend fun add(newSessionWithSections: SessionWithSections) {
        sessionDao.insert(newSessionWithSections)
    }

    /** Delete */
    suspend fun delete(session: Session) {
        sessionDao.delete(session)
    }

    suspend fun delete(sessions: List<Session>) {
        sessionDao.delete(sessions)
    }
}