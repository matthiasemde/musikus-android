/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.repository

import androidx.room.Transaction
import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalDescriptionDao
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.GoalInstanceDao
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.utils.TimeProvider
import app.musikus.utils.getStartOfDay
import app.musikus.utils.getStartOfMonth
import app.musikus.utils.getStartOfWeek
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import kotlin.time.Duration

interface GoalRepository {
    val timeProvider: TimeProvider
    val currentGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
    val allGoals: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
    val lastFiveCompletedGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    /** Mutators */
    /** Add */
    suspend fun add(
        goalDescriptionCreationAttributes: GoalDescriptionCreationAttributes,
        startingTimeframe : ZonedDateTime = timeProvider.now(),
        libraryItems: List<LibraryItem>?,
        target: Duration,
    )

    /** Edit */
    suspend fun editGoalTarget(
        goal: GoalInstance,
        newTarget: Duration,
    )


    /** Pause / Unpause */
    suspend fun pause(goal: GoalDescription)
    suspend fun pause(goals: List<GoalDescription>)

    suspend fun unpause(goal: GoalDescription)
    suspend fun unpause(goals: List<GoalDescription>)

    /** Archive / Unarchive */
    suspend fun archive(goal: GoalDescription)
    suspend fun archive(goals: List<GoalDescription>)

    suspend fun unarchive(goal: GoalDescription)
    suspend fun unarchive(goals: List<GoalDescription>)

    /** Delete / Restore */
    suspend fun delete(goal: GoalDescription)
    suspend fun delete(goals: List<GoalDescription>)

    suspend fun restore(goal: GoalDescription)
    suspend fun restore(goals: List<GoalDescription>)

    /** Clean */
    suspend fun clean()

    /** Update Goals */
    suspend fun updateGoals()
}

class GoalRepositoryImpl(
    private val goalInstanceDao : GoalInstanceDao,
    private val goalDescriptionDao : GoalDescriptionDao,
    override val timeProvider: TimeProvider
) : GoalRepository {

    private suspend fun update(
        goalsWithUpdateAttributes: List<Pair<GoalDescription, GoalDescriptionUpdateAttributes>>,
    ) {
        goalDescriptionDao.update(
            goalsWithUpdateAttributes.map { (goalDescription, updateAttributes) ->
                goalDescription.id to updateAttributes
            }
        )
    }


    override val currentGoals = goalInstanceDao.getActiveInstancesWithDescriptionWithLibraryItems()
    override val allGoals = goalDescriptionDao.getAllWithInstancesAndLibraryItems()
    override val lastFiveCompletedGoals = goalInstanceDao.getLastNCompletedWithDescriptionsWithLibraryItems(5)


    /** Mutators */

    /** Add */
    override suspend fun add(
        goalDescriptionCreationAttributes: GoalDescriptionCreationAttributes,
        startingTimeframe : ZonedDateTime,
        libraryItems: List<LibraryItem>?,
        target: Duration,
    ) {
        val start = when(goalDescriptionCreationAttributes.periodUnit) {
            GoalPeriodUnit.DAY -> getStartOfDay(dateTime = startingTimeframe)
            GoalPeriodUnit.WEEK -> getStartOfWeek(dateTime = startingTimeframe)
            GoalPeriodUnit.MONTH -> getStartOfMonth(dateTime = startingTimeframe)
        }

        goalDescriptionDao.insert(
            description = GoalDescriptionModel(
                type = goalDescriptionCreationAttributes.type,
                repeat = goalDescriptionCreationAttributes.repeat,
                periodInPeriodUnits = goalDescriptionCreationAttributes.periodInPeriodUnits,
                periodUnit = goalDescriptionCreationAttributes.periodUnit,
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                startTimestamp = start,
                target = target
            ),
            libraryItemIds = libraryItems?.map { it.id },
        )
    }

    private suspend fun createInstance(
        description: GoalDescription,
        timeframe: ZonedDateTime,
        target: Duration,
    ) {
        val start = when(description.periodUnit) {
            GoalPeriodUnit.DAY -> getStartOfDay(dateTime = timeframe)
            GoalPeriodUnit.WEEK -> getStartOfWeek(dateTime = timeframe)
            GoalPeriodUnit.MONTH -> getStartOfMonth(dateTime = timeframe)
        }

        goalInstanceDao.insert(
            description.id,
            GoalInstanceCreationAttributes(
                startTimestamp = start,
                target = target
            )
        )
    }


    /** Edit */
    override suspend fun editGoalTarget(
        goal: GoalInstance,
        newTarget: Duration,
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
    override suspend fun pause(goal: GoalDescription) {
        pause(listOf(goal))
    }

    @Transaction
    override suspend fun pause(goals: List<GoalDescription>) {
        update(
            goals.map {
                it to GoalDescriptionUpdateAttributes(paused = true)
            },
        )
    }

    @Transaction
    override suspend fun unpause(goal: GoalDescription) {
        unpause(listOf(goal))
    }

    @Transaction
    override suspend fun unpause(goals: List<GoalDescription>) {
        update(
            goals.map {
                it to GoalDescriptionUpdateAttributes(paused = false)
            }
        )
    }

    /** Archive / Unarchive */

    override suspend fun archive(goal: GoalDescription) {
        archive(listOf(goal))
    }

    override suspend fun archive(goals: List<GoalDescription>) {
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

    override suspend fun unarchive(goal: GoalDescription) {
        unarchive(listOf(goal))
    }

    override suspend fun unarchive(goals: List<GoalDescription>) {
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

    override suspend fun delete(goal: GoalDescription) {
        delete(listOf(goal))
    }

    override suspend fun delete(goals: List<GoalDescription>) {
        goalDescriptionDao.delete(goals.map { it.id })
    }

    override suspend fun restore(goal: GoalDescription) {
        restore(listOf(goal))
    }

    override suspend fun restore(goals: List<GoalDescription>) {
        goalDescriptionDao.restore(goals.map { it.id })
    }

    /** Clean */

    override suspend fun clean() {
        goalDescriptionDao.clean()
    }


    /**
     * Utility functions
     * */


    /** Update Goals */
    override suspend fun updateGoals() {
        while(true) {
            val outdatedGoalsWithEndTimestamps = goalInstanceDao
                .getActiveInstancesWithDescription()
                .map {
                    it to it.description.endOfInstanceInLocalTimezone(it.instance)
                }.filter { (_, endTimestamp) ->
                    timeProvider.now() > endTimestamp
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