/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.repository

import androidx.room.Transaction
import app.musikus.database.MusikusDatabase
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.utils.getCurrentDateTime
import java.time.ZonedDateTime

class GoalRepository(
    database: MusikusDatabase
) {
    private val goalInstanceDao = database.goalInstanceDao
    private val goalDescriptionDao = database.goalDescriptionDao

    private suspend fun update(
        goalsWithUpdateAttributes: List<Pair<GoalDescription, GoalDescriptionUpdateAttributes>>,
    ) {
        goalDescriptionDao.update(
            goalsWithUpdateAttributes.map { (goalDescription, updateAttributes) ->
                goalDescription.id to updateAttributes
            }
        )
    }


    val currentGoals = goalInstanceDao.getActiveInstancesWithDescriptionWithLibraryItems()
    val allGoals = goalDescriptionDao.getAllWithInstancesAndLibraryItems()
    val lastFiveCompletedGoals = goalInstanceDao.getLastNCompletedWithDescriptionsWithLibraryItems(5)


    /** Mutators */

    /** Add */
    suspend fun add(
        goalDescriptionCreationAttributes: GoalDescriptionCreationAttributes,
        startingTimeframe : ZonedDateTime = ZonedDateTime.now(),
        libraryItems: List<LibraryItem>?,
        target: Int,
    ) = goalDescriptionDao.insert(
        goalDescription = GoalDescriptionModel(
            type = goalDescriptionCreationAttributes.type,
            repeat = goalDescriptionCreationAttributes.repeat,
            periodInPeriodUnits = goalDescriptionCreationAttributes.periodInPeriodUnits,
            periodUnit = goalDescriptionCreationAttributes.periodUnit,
        ),
        startingTimeframe = startingTimeframe,
        libraryItemIds = libraryItems?.map { it.id },
        target = target,
    )

    private suspend fun createInstance(
        description: GoalDescription,
        timeframe: ZonedDateTime,
        target: Int,
    ) {
        goalInstanceDao.insert(
            description,
            timeframe,
            target,
        )
    }


    /** Edit */
    suspend fun editGoalTarget(
        goal: GoalInstance,
        newTarget: Int,
    ) {
        goalInstanceDao.update(
            goal.id,
            GoalInstanceUpdateAttributes(target = newTarget)
        )
    }

    private suspend fun markInstanceAsRenewed(
        goal: GoalInstance,
        endTimestamp: ZonedDateTime
    ) {
        goalInstanceDao.update(
            goal.id,
            GoalInstanceUpdateAttributes(
                renewed = true,
                endTimestamp = endTimestamp
            )
        )
    }


    /** Pause / Unpause */
    @Transaction
    suspend fun pause(goal: GoalDescription) {
        pause(listOf(goal))
    }

    @Transaction
    suspend fun pause(goals: List<GoalDescription>) {
        update(
            goals.map {
                it to GoalDescriptionUpdateAttributes(paused = true)
            },
        )
    }

    @Transaction
    suspend fun unpause(goal: GoalDescription) {
        unpause(listOf(goal))
    }

    @Transaction
    suspend fun unpause(goals: List<GoalDescription>) {
        update(
            goals.map {
                it to GoalDescriptionUpdateAttributes(paused = false)
            }
        )
    }

    /** Archive / Unarchive */

    suspend fun archive(goal: GoalDescription) {
        archive(listOf(goal))
    }

    suspend fun archive(goals: List<GoalDescription>) {
        update(
            goals.map {
                it to GoalDescriptionUpdateAttributes(archived = true)
            },
        )
        goals.forEach {
            goalInstanceDao.getLatest(it.id)?.let { latestInstance ->
                goalInstanceDao.update(
                    latestInstance.id,
                    GoalInstanceUpdateAttributes(
                        endTimestamp = it.endOfInstanceInLocalTimezone(latestInstance)
                    )
                )
            }
        }
    }

    suspend fun unarchive(goal: GoalDescription) {
        unarchive(listOf(goal))
    }

    suspend fun unarchive(goals: List<GoalDescription>) {
        update(
            goals.map {
                it to GoalDescriptionUpdateAttributes(archived = false)
            }
        )
        goals.forEach {
            goalInstanceDao.getLatest(it.id)?.let { latestInstance ->
                goalInstanceDao.update(
                    latestInstance.id,
                    GoalInstanceUpdateAttributes(
                        endTimestamp = it.endOfInstanceInLocalTimezone(latestInstance)
                    )
                )
            }
        }
    }

    /** Delete / Restore */

    suspend fun delete(goal: GoalDescription) {
        delete(listOf(goal))
    }

    suspend fun delete(goals: List<GoalDescription>) {
        goalDescriptionDao.delete(goals.map { it.id })
    }

    suspend fun restore(goal: GoalDescription) {
        restore(listOf(goal))
    }

    suspend fun restore(goals: List<GoalDescription>) {
        goalDescriptionDao.restore(goals.map { it.id })
    }

    /** Clean */

    suspend fun clean() {
        goalDescriptionDao.clean()
    }


    /**
     * Utility functions
     * */


    /** Update Goals */
    suspend fun updateGoals() {
        while(true) {
            val outdatedGoalsWithEndTimestamps = goalInstanceDao
                .getActiveInstancesWithDescription()
                .map {
                    it to it.description.endOfInstanceInLocalTimezone(it.instance)
                }.filter { (_, endTimestamp) ->
                    getCurrentDateTime() > endTimestamp
                }

            if (outdatedGoalsWithEndTimestamps.isEmpty()) return

            // while there are still outdated goals, keep looping and adding new ones
            outdatedGoalsWithEndTimestamps.forEach { (outdatedGoal, endTimestamp) ->
                val outdatedInstance = outdatedGoal.instance
                val description = outdatedGoal.description
                if (description.repeat) {

                    // create a new goal with the same description and target
                    createInstance(
                        description,
                        endTimestamp,
                        outdatedInstance.target
                    )

                    // if the outdated instance belonged to a paused goal delete it...
                    if (description.paused) {
                        goalInstanceDao.delete(outdatedInstance.id)

                    // otherwise mark it as renewed
                    } else {
                        markInstanceAsRenewed(outdatedInstance, endTimestamp)
                    }

                } else if (!description.archived) {
                    // one shot goals are archived after they are outdated
                    archive(description)
                }
            }
        }
    }
}