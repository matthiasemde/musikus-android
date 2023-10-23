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

package app.musikus.database.daos

import androidx.room.*
import app.musikus.database.*
import app.musikus.database.entities.*
import java.util.*

@Dao
abstract class GoalDescriptionDao(
    private val database : PTDatabase
) : SoftDeleteDao<
    GoalDescription,
    GoalDescriptionUpdateAttributes,
    GoalDescription
>(
    tableName = "goal_description",
    database = database,
    displayAttributes = listOf("name", "type", "progress_type", "archived", "paused", "order")
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: GoalDescription,
        updateAttributes: GoalDescriptionUpdateAttributes
    ): GoalDescription = super.applyUpdateAttributes(old, updateAttributes).apply {
        paused = updateAttributes.paused ?: old.paused
        archived = updateAttributes.archived ?: old.archived
        order = updateAttributes.order ?: old.order
    }


    /**
     * @Insert
     */

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertGoalDescriptionLibraryItemCrossRef(
        crossRef: GoalDescriptionLibraryItemCrossRef
    ): Long

    @Transaction
    open suspend fun insert(
        goalDescriptionWithLibraryItems: GoalDescriptionWithLibraryItems,
        target: Int,
    ) : GoalInstance? {

        // Create the first instance of the newly created goal description
        val firstGoalInstance = goalDescriptionWithLibraryItems.description.createInstance(
            Calendar.getInstance(),
            target
        )

        insert(goalDescriptionWithLibraryItems.description)
//        database.goalInstanceDao.insert(firstGoalInstance) TODO:fix
//        goalDescriptionWithLibraryItems.libraryItems.forEach { libraryItem ->
//            insertGoalDescriptionLibraryItemCrossRef(
//                GoalDescriptionLibraryItemCrossRef(
//                    goalDescriptionId = goalDescriptionWithLibraryItems.description.id,
//                    libraryItemId = libraryItem.id
//                )
//            )
//        }

        return firstGoalInstance
    }

    /**
     * @Queries
     */

    @Transaction
    @Query(
        "SELECT * FROM goal_description_library_item_cross_ref " +
        "WHERE goal_description_id=:goalDescriptionId"
    )
    abstract suspend fun getGoalDescriptionLibraryItemCrossRefs(
        goalDescriptionId: UUID
    ) : List<GoalDescriptionLibraryItemCrossRef>


    @Transaction
    @Query("SELECT * FROM goal_description WHERE id=:goalDescriptionId")
    abstract suspend fun getWithLibraryItems(goalDescriptionId: UUID)
        : GoalDescriptionWithLibraryItems?

    @Transaction
    @Query("SELECT * FROM goal_description")
    abstract suspend fun getAllWithLibraryItems(): List<GoalDescriptionWithLibraryItems>

    @Query(
        "SELECT * FROM goal_description " +
        "WHERE (archived=0 OR archived=:checkArchived) " +
        "AND type=:type"
    )
    abstract suspend fun getGoalDescriptions(
        checkArchived : Boolean = false,
        type : GoalType
    ) : List<GoalDescription>


//    /**
//     * Goal Progress Update Utility
//     */
//
//    @Transaction
//    open suspend fun computeGoalProgressForSession(
//        session: SessionWithSectionsWithLibraryItemsWithGoalDescriptions,
//        checkArchived: Boolean = false,
//    ) : Map<UUID, Int> {
//        var totalSessionDuration = 0
//
//        // goalProgress maps the goalDescription-id to its progress
//        val goalProgress = mutableMapOf<UUID, Int>()
//
//        // go through all the sections in the session...
//        session.sections.forEach { (section, libraryItemWithGoalDescriptions) ->
//            // ... using the respective libraryItems, find the goals,
//            // to which the sections are contributing to...
//            val (_, goalDescriptions) = libraryItemWithGoalDescriptions
//
//            // ... and loop through those goals, summing up the duration
//            goalDescriptions.filter {d -> checkArchived || !d.archived}.forEach { description ->
//                when (description.progressType) {
//                    GoalProgressType.TIME -> goalProgress[description.id] =
//                        goalProgress[description.id] ?: (0 + (section.duration ?: 0))
//                    GoalProgressType.SESSION_COUNT -> goalProgress[description.id] = 1
//                }
//            }
//
//            // simultaneously sum up the total session duration
//            totalSessionDuration += section.duration ?: 0
//        }
//
//        // query all goal descriptions which have type NON-SPECIFIC
//        getGoalDescriptions(
//            checkArchived,
//            GoalType.NON_SPECIFIC,
//        ).forEach { totalTimeGoal ->
//            goalProgress[totalTimeGoal.id] = when (totalTimeGoal.progressType) {
//                GoalProgressType.TIME -> totalSessionDuration
//                GoalProgressType.SESSION_COUNT -> 1
//            }
//        }
//        return goalProgress
//    }
}

