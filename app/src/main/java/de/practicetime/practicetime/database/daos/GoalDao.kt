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

import android.util.Log
import androidx.room.*
import de.practicetime.practicetime.database.*
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.utils.getCurrTimestamp
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
abstract class GoalDescriptionDao(
    private val database : PTDatabase
) : BaseDao<GoalDescription>(
    tableName = "goal_description",
    database = database
) {

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
        database.goalInstanceDao.insert(firstGoalInstance)
        goalDescriptionWithLibraryItems.libraryItems.forEach { libraryItem ->
            insertGoalDescriptionLibraryItemCrossRef(
                GoalDescriptionLibraryItemCrossRef(
                    goalDescriptionId = goalDescriptionWithLibraryItems.description.id,
                    libraryItemId = libraryItem.id
                )
            )
        }

        return firstGoalInstance
    }

    /**
     * @DELETE / archive
     */

    suspend fun archive(goalDescription: GoalDescription) {
        goalDescription.archived = true
        update(goalDescription)
    }

    suspend fun archive(goalDescriptions: List<GoalDescription>) {
        goalDescriptions.forEach { it.archived = true }
        update(goalDescriptions)
    }

    suspend fun getAndArchive(goalDescriptionIds: List<UUID>) {
        archive(get(goalDescriptionIds))
    }

    /**
     * @Update
     */

    @Transaction
    open suspend fun updateTarget(goalDescriptionId: UUID, newTarget: Int) {
        database.goalInstanceDao.apply {
            get(
                goalDescriptionId = goalDescriptionId,
                from = getCurrTimestamp(),
            ).forEach {
                it.target = newTarget
                update(it)
            }
        }
    }

    @Transaction
    open suspend fun unarchive(archivedGoal: GoalInstanceWithDescriptionWithLibraryItems) {
        val (instance, descriptionWithLibraryItems) = archivedGoal
        val description = descriptionWithLibraryItems.description

        description.archived = false
        update(description)
        if(instance.startTimestamp + instance.periodInSeconds > getCurrTimestamp()) {
            instance.renewed = false
            database.goalInstanceDao.update(instance)
        } else {
//            database.goalInstanceDao.insertWithProgress(
//                description.createInstance(Calendar.getInstance(), instance.target)
//            )
        }
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

    @Transaction
    @Query("SELECT * FROM goal_description WHERE archived=1")
    abstract suspend fun getArchivedWithLibraryItems(
    ) : List<GoalDescriptionWithLibraryItems>


    /**
     * Goal Progress Update Utility
     */

    @Transaction
    open suspend fun computeGoalProgressForSession(
        session: SessionWithSectionsWithLibraryItemsWithGoalDescriptions,
        checkArchived: Boolean = false,
    ) : Map<UUID, Int> {
        var totalSessionDuration = 0

        // goalProgress maps the goalDescription-id to its progress
        val goalProgress = mutableMapOf<UUID, Int>()

        // go through all the sections in the session...
        session.sections.forEach { (section, libraryItemWithGoalDescriptions) ->
            // ... using the respective libraryItems, find the goals,
            // to which the sections are contributing to...
            val (_, goalDescriptions) = libraryItemWithGoalDescriptions

            // ... and loop through those goals, summing up the duration
            goalDescriptions.filter {d -> checkArchived || !d.archived}.forEach { description ->
                when (description.progressType) {
                    GoalProgressType.TIME -> goalProgress[description.id] =
                        goalProgress[description.id] ?: (0 + (section.duration ?: 0))
                    GoalProgressType.SESSION_COUNT -> goalProgress[description.id] = 1
                }
            }

            // simultaneously sum up the total session duration
            totalSessionDuration += section.duration ?: 0
        }

        // query all goal descriptions which have type NON-SPECIFIC
        getGoalDescriptions(
            checkArchived,
            GoalType.NON_SPECIFIC,
        ).forEach { totalTimeGoal ->
            goalProgress[totalTimeGoal.id] = when (totalTimeGoal.progressType) {
                GoalProgressType.TIME -> totalSessionDuration
                GoalProgressType.SESSION_COUNT -> 1
            }
        }
        return goalProgress
    }
}

/**
 *  @Dao Goal Instance Dao
 */

@Dao
abstract class GoalInstanceDao(
    private val database : PTDatabase
) : BaseDao<GoalInstance>(
    tableName = "goal_instance",
    database = database
) {

    /**
     * @Insert
     */

//    @Transaction
//    open suspend fun insertWithProgress(
//        goalInstance: GoalInstance
//    ){
//        database.sessionDao.getSessionsContainingSectionFromTimeFrame(
//            goalInstance.startTimestamp,
//            goalInstance.startTimestamp + goalInstance.periodInSeconds
//        ).filter { s -> s.sections.first().timestamp >= goalInstance.startTimestamp }
//        .forEach { s ->
//            database.goalDescriptionDao.computeGoalProgressForSession(
//                database.sessionDao.getWithSectionsWithLibraryItemsWithGoals(s.session.id)
//            ).also { progress ->
//                goalInstance.progress += progress[goalInstance.goalDescriptionId] ?: 0
//            }
//        }
//        insert(goalInstance)
//    }


    /**
     * @Update
     */

    @Transaction
    open suspend fun renewGoalInstance(id: UUID) {
        get(id)?.also { g ->
            g.renewed = true
            update(g)
        } ?: Log.e("GOAL_INSTANCE_DAO", "Trying to renew goal instance with id: $id failed")
    }

    /**
     * @Queries
     */

    /**
     * Get all [GoalInstance] entities matching a specific pattern
     * @param goalDescriptionIds
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp] / 1000L
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @Query(
        "SELECT * FROM goal_instance " +
        "WHERE goal_description_id IN (:goalDescriptionIds)" +
        "AND (" +
            "start_timestamp>:from AND NOT :inclusiveFrom OR " +
            "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
        ")" +
        "AND (" +
            "start_timestamp<:to AND :inclusiveTo OR " +
            "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
        ")"
    )
    abstract suspend fun get(
        goalDescriptionIds: List<UUID>,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstance>

    suspend fun get(
        goalDescriptionId: UUID,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstance> {
        return get(
            goalDescriptionIds = listOf(goalDescriptionId),
            from = from,
            to = to,
            inclusiveFrom = inclusiveFrom,
            inclusiveTo = inclusiveTo
        )
    }

    /**
     * Get all [GoalInstanceWithDescription] entities matching a specific pattern
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp]
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @Transaction
    @Query(
        "SELECT * FROM goal_instance WHERE (" +
            "start_timestamp>:from AND NOT :inclusiveFrom OR " +
            "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
        ")" +
        "AND (" +
            "start_timestamp<:to AND :inclusiveTo OR " +
            "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
        ")"
    )
    abstract suspend fun getWithDescription(
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ) : List<GoalInstanceWithDescription>

    /**
     * Get all [GoalInstanceWithDescriptionWithLibraryItems] entities matching a specific pattern
     * @param goalDescriptionId
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp]
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @Transaction
    @Query(
        "SELECT * FROM goal_instance " +
                "WHERE goal_description_id=:goalDescriptionId " +
                "AND (" +
                "start_timestamp>:from AND NOT :inclusiveFrom OR " +
                "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
                ")" +
                "AND (" +
                "start_timestamp<:to AND :inclusiveTo OR " +
                "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
                ")"
    )
    abstract suspend fun getWithDescriptionWithLibraryItems(
        goalDescriptionId: UUID,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstanceWithDescriptionWithLibraryItems>

    @Transaction
    @Query(
        "SELECT * FROM goal_instance " +
        "WHERE renewed=0 " +
        "AND start_timestamp + period_in_seconds < :to"
    )
    abstract suspend fun getOutdatedWithDescriptions(
        to : Long = getCurrTimestamp()
    ) : List<GoalInstanceWithDescription>

    @Transaction
    @Query(
        "SELECT * FROM goal_instance WHERE " +
        "start_timestamp < :now " +
        "AND start_timestamp+period_in_seconds > :now " +
        "AND goal_description_id IN (" +
            "SELECT id FROM goal_description WHERE " +
            "archived=0 OR :checkArchived" +
        ")"
    )
    abstract fun getWithDescriptionsWithLibraryItems(
        checkArchived : Boolean = false,
        now : Long = getCurrTimestamp(),
    ) : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    @Transaction
    @Query(
        "SELECT * FROM goal_instance WHERE " +
        "goal_description_id IN (:goalDescriptionIds) " +
        "AND start_timestamp < :now " +
        "AND start_timestamp+period_in_seconds > :now " +
        "AND goal_description_id IN (" +
            "SELECT id FROM goal_description WHERE " +
            "archived=0 OR :checkArchived" +
        ")"
    )
    abstract suspend fun getWithDescriptionsWithLibraryItems(
        goalDescriptionIds: List<UUID>,
        checkArchived : Boolean = false,
        now : Long = getCurrTimestamp(),
    ) : List<GoalInstanceWithDescriptionWithLibraryItems>

    @Query(
        "Select * FROM goal_instance WHERE " +
        "goal_description_id=:goalDescriptionId AND " +
        "start_timestamp=(" +
            "SELECT MAX(start_timestamp) FROM goal_instance WHERE " +
            "goal_description_id = :goalDescriptionId" +
        ")"
    )
    abstract suspend fun getLatest(
        goalDescriptionId: UUID
    ): GoalInstance
}
