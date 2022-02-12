package de.practicetime.practicetime.database.daos

import android.util.Log
import androidx.room.*
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.database.BaseDao
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.utils.getCurrTimestamp
import java.util.*

@Dao
abstract class GoalDescriptionDao : BaseDao<GoalDescription>(
    tableName = "goal_description"
) {

    /**
     * @Insert
     */

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertGoalDescriptionCategoryCrossRef(
        crossRef: GoalDescriptionCategoryCrossRef
    ): Long

    @Transaction
    open suspend fun insertAndGetGoalDescriptionWithCategories(
        goalDescriptionWithCategories: GoalDescriptionWithCategories
    ) : GoalDescriptionWithCategories? {
        return insertAndGet(
            goalDescriptionWithCategories.description
        )?.let { description ->

            // for every category linked with the goal...
            for (category in goalDescriptionWithCategories.categories) {
                // insert a row in the cross reference table
                insertGoalDescriptionCategoryCrossRef(
                    GoalDescriptionCategoryCrossRef(
                        description.id,
                        category.id,
                    )
                )
            }
            return GoalDescriptionWithCategories(
                description,
                goalDescriptionWithCategories.categories
            )
        }
    }

    @Transaction
    open suspend fun insertGoal(
        goalDescriptionWithCategories: GoalDescriptionWithCategories,
        target: Int,
    ) : GoalInstanceWithDescriptionWithCategories? {

        return insertAndGetGoalDescriptionWithCategories(
            goalDescriptionWithCategories
        )?.let { newGoalDescriptionWithCategories ->
            // Create the first instance of the newly created goal description
            PracticeTime.goalInstanceDao.insertUpdateAndGet(
                newGoalDescriptionWithCategories.description.createInstance(
                    Calendar.getInstance(),
                    target
                )
            )?.let { newGoalInstance ->
                GoalInstanceWithDescriptionWithCategories(
                    newGoalInstance,
                    newGoalDescriptionWithCategories
                )
            }
        }
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

    suspend fun getAndArchive(goalDescriptionIds: List<Long>) {
        archive(get(goalDescriptionIds))
    }

    @Transaction
    open suspend fun deleteGoal(goalDescriptionId: Long) {
        // to delete a goal, first fetch all instances from the database and delete them
        PracticeTime.goalInstanceDao.apply {
            get(
                goalDescriptionId = goalDescriptionId,
                from = 0L
            ).forEach {
                delete(it)
            }
        }
        // we also need to remove all entries in the cross reference table
        deleteGoalDescriptionCategoryCrossRefs(
            getGoalDescriptionCategoryCrossRefs(goalDescriptionId)
        )
        // finally, we can delete the description
        getAndDelete(goalDescriptionId)
    }

    suspend fun deleteGoals(goalDescriptionIds: List<Long>) {
        goalDescriptionIds.forEach { deleteGoal(it) }
    }

    @Delete
    abstract suspend fun deleteGoalDescriptionCategoryCrossRef(
        crossRef: GoalDescriptionCategoryCrossRef
    )

    @Delete
    abstract suspend fun deleteGoalDescriptionCategoryCrossRefs(
        crossRefs: List<GoalDescriptionCategoryCrossRef>
    )


    /**
     * @Update
     */

    @Transaction
    open suspend fun updateTarget(goalDescriptionId: Long, newTarget: Int) {
        PracticeTime.goalInstanceDao.apply {
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
    open suspend fun unarchive(archivedGoal: GoalInstanceWithDescriptionWithCategories) {
        val (instance, descriptionWithCategories) = archivedGoal
        val description = descriptionWithCategories.description

        description.archived = false
        update(description)
        if(instance.startTimestamp + instance.periodInSeconds > getCurrTimestamp()) {
            instance.renewed = false
            PracticeTime.goalInstanceDao.update(instance)
        } else {
            PracticeTime.goalInstanceDao.insertUpdateAndGet(
                description.createInstance(Calendar.getInstance(), instance.target)
            )
        }
    }

    /**
     * @Queries
     */

    @Transaction
    @Query(
        "SELECT * FROM goal_description_category_cross_ref " +
        "WHERE goal_description_id=:goalDescriptionId"
    )
    abstract suspend fun getGoalDescriptionCategoryCrossRefs(
        goalDescriptionId: Long
    ) : List<GoalDescriptionCategoryCrossRef>


    @Transaction
    @Query("SELECT * FROM goal_description WHERE id=:goalDescriptionId")
    abstract suspend fun getWithCategories(goalDescriptionId: Long)
        : GoalDescriptionWithCategories?

    @Transaction
    @Query("SELECT * FROM goal_description")
    abstract suspend fun getAllWithCategories(): List<GoalDescriptionWithCategories>

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
    abstract suspend fun getArchivedWithCategories(
    ) : List<GoalDescriptionWithCategories>


    /**
     * Goal Progress Update Utility
     */

    @Transaction
    open suspend fun computeGoalProgressForSession(
        session: SessionWithSectionsWithCategoriesWithGoalDescriptions,
        checkArchived: Boolean = false,
    ) : Map<Long, Int> {
        var totalSessionDuration = 0

        // goalProgress maps the goalDescription-id to its progress
        val goalProgress = mutableMapOf<Long, Int>()

        // go through all the sections in the session...
        session.sections.forEach { (section, categoryWithGoalDescriptions) ->
            // ... using the respective categories, find the goals,
            // to which the sections are contributing to...
            val (_, goalDescriptions) = categoryWithGoalDescriptions

            // ... and loop through those goals, summing up the duration
            goalDescriptions.filter {d -> checkArchived || !d.archived}.forEach { description ->
                when (description.progressType) {
                    GoalProgressType.TIME -> goalProgress[description.id] =
                        goalProgress[description.id] ?: 0 + (section.duration ?: 0)
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
abstract class GoalInstanceDao : BaseDao<GoalInstance>(tableName = "goal_instance") {

    /**
     * @Insert
     */

    @Transaction
    open suspend fun insertUpdateAndGet(
        goalInstance: GoalInstance
    ) : GoalInstance? {
        return insertAndGet(
            goalInstance
        )?.let { newInstance ->
            PracticeTime.sessionDao.getSessionsContainingSectionFromTimeFrame(
                newInstance.startTimestamp,
                newInstance.startTimestamp + newInstance.periodInSeconds
            ).filter { s -> s.sections.first().timestamp >= newInstance.startTimestamp }
            .forEach { s ->
                PracticeTime.goalDescriptionDao.computeGoalProgressForSession(
                    PracticeTime.sessionDao.getWithSectionsWithCategoriesWithGoals(s.session.id)
                ).also { progress ->
                    newInstance.progress += progress[newInstance.goalDescriptionId] ?: 0
                }
            }
            update(newInstance)
            newInstance
        }
    }


    /**
     * @Update
     */

    @Transaction
    open suspend fun renewGoalInstance(id: Long) {
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
        goalDescriptionIds: List<Long>,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstance>

    suspend fun get(
        goalDescriptionId: Long,
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
     * Get all [GoalInstanceWithDescriptionWithCategories] entities matching a specific pattern
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
    abstract suspend fun getWithDescriptionWithCategories(
        goalDescriptionId: Long,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstanceWithDescriptionWithCategories>

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
    abstract suspend fun getWithDescriptionsWithCategories(
        checkArchived : Boolean = false,
        now : Long = getCurrTimestamp(),
    ) : List<GoalInstanceWithDescriptionWithCategories>

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
    abstract suspend fun getWithDescriptionsWithCategories(
        goalDescriptionIds: List<Long>,
        checkArchived : Boolean = false,
        now : Long = getCurrTimestamp(),
    ) : List<GoalInstanceWithDescriptionWithCategories>

    @Query(
        "Select * FROM goal_instance WHERE " +
        "goal_description_id=:goalDescriptionId AND " +
        "start_timestamp=(" +
            "SELECT MAX(start_timestamp) FROM goal_instance WHERE " +
            "goal_description_id = :goalDescriptionId" +
        ")"
    )
    abstract suspend fun getLatest(
        goalDescriptionId: Long
    ): GoalInstance

}