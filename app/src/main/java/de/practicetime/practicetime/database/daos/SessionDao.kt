/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.daos

import androidx.room.*
import de.practicetime.practicetime.database.*
import de.practicetime.practicetime.database.entities.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
abstract class SessionDao(
    private val database : PTDatabase
) : BaseDao<Session>(
    tableName = "session",
    database = database
) {

    /**
     * @Insert
      */

    @Transaction
    open suspend fun insert(
        sessionWithSections: SessionWithSections,
    ) {
        insert(sessionWithSections.session)
        database.sectionDao.insert(sessionWithSections.sections.map {
            it.copy(sessionId = sessionWithSections.session.id) }
        )
    }


    /**
     * @Delete / archive
     */

//    override suspend fun delete(rows: List<Session>) {
//        throw Exception("Illegal operation. Call delete with updated goal instances instead")
//    }

    @Transaction
    open suspend fun delete(sessionId: UUID, updatedGoalInstances: List<GoalInstance>) {
        get(sessionId)?.let { session ->
            updatedGoalInstances.forEach { database.goalInstanceDao.update(it) }
            database.sectionDao.apply {
                getFromSession(sessionId).forEach { delete(it) }
            }
                delete(session)
        } ?: throw Exception("Tried to delete non existent session with id $sessionId")
    }


    /**
     * @Queries
     */

    @Transaction
    @Query("SELECT * FROM session WHERE id=:sessionId")
    abstract suspend fun getWithSections(sessionId: UUID): SessionWithSections

    @Transaction
    @Query("SELECT * FROM session WHERE id=:sessionId")
    abstract suspend fun getWithSectionsWithLibraryItems(
        sessionId: UUID
    ): SessionWithSectionsWithLibraryItems

    @Transaction
    @Query("SELECT * FROM session")
    abstract fun getAllWithSectionsWithLibraryItems(
    ): Flow<List<SessionWithSectionsWithLibraryItems>>

    @Transaction
    @Query("SELECT * FROM session WHERE id=:sessionId")
    abstract suspend fun getWithSectionsWithLibraryItemsWithGoals(
        sessionId: UUID
    ) : SessionWithSectionsWithLibraryItemsWithGoalDescriptions

    @Transaction
    @Query(
        "SELECT * FROM session WHERE " +
        "id IN (" +
            "SELECT session_id FROM section WHERE " +
            "timestamp > :from AND timestamp < :to" +
        ")"
    )
    abstract suspend fun getSessionsContainingSectionFromTimeFrame(
        from: Long,
        to: Long,
    ): List<SessionWithSections>

    @Transaction
    open suspend fun update(
        sessionId: UUID,
        newRating: Int,
        newSections: List<SectionWithLibraryItem>,
        newComment: String,
    ) {
        // the difference session will save the difference in the section duration
        // between the original session and the edited sections
        val sessionWithSectionsWithLibraryItemsWithGoals =
            getWithSectionsWithLibraryItemsWithGoals(sessionId)

        sessionWithSectionsWithLibraryItemsWithGoals.sections.forEach { (section, _) ->
            section.duration = (newSections.find {
                it.section.id == section.id
            }?.section?.duration ?: 0) - (section.duration ?: 0)
        }

        val goalProgress = database.goalDescriptionDao.computeGoalProgressForSession(
            sessionWithSectionsWithLibraryItemsWithGoals,
            checkArchived = true
        )

        // get all active goal instances at the time of the session
        database.goalInstanceDao.apply {
            get(
                goalDescriptionIds = goalProgress.keys.toList(),
                from = sessionWithSectionsWithLibraryItemsWithGoals.sections.first().section.timestamp
                // add the progress
            ).onEach { instance ->
                goalProgress[instance.goalDescriptionId].also { progress ->
                    if (progress != null) {
                        // progress should not get lower than 0
                        instance.progress = maxOf(0 , instance.progress + progress)
                    }
                }
                update(instance)
            }
        }

        // update all sections
        newSections.forEach { (section, _) ->
            database.sectionDao.update(section)
        }

        sessionWithSectionsWithLibraryItemsWithGoals.session.apply {
            comment = newComment
            rating = newRating
        }

        update(sessionWithSectionsWithLibraryItemsWithGoals.session)
    }
}
