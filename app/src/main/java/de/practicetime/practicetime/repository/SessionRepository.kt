package de.practicetime.practicetime.repository

import de.practicetime.practicetime.database.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.SessionWithSections

class SessionRepository(
    database: PTDatabase
) {
    private val sessionDao = database.sessionDao
    private val sectionDao = database.sectionDao


    /** Accessors */
    val sessions = sessionDao.getAllAsFlow()
    val sections = sectionDao.getAllAsFlow()

    suspend fun sectionsForGoal (goal: GoalInstanceWithDescriptionWithLibraryItems) =
        sectionDao.get(
            startTimeStamp = goal.instance.startTimestamp,
            endTimeStamp = goal.instance.startTimestamp + goal.instance.periodInSeconds,
            itemIds = goal.description.libraryItems.map { it.id }
        )

    /** Mutators */
    /** Add */
    suspend fun addSession(newSessionWithSections: SessionWithSections) {
        sessionDao.insert(newSessionWithSections)
    }
}