/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.repository

import androidx.room.Transaction
import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.datastore.GoalsSortMode
import app.musikus.datastore.SortDirection
import java.util.Calendar

class GoalRepository(
    database: MusikusDatabase
) {
    private val goalInstanceDao = database.goalInstanceDao
    private val goalDescriptionDao = database.goalDescriptionDao

    private suspend fun update(
        goals: List<GoalDescription>,
        updateAttributes: GoalDescriptionUpdateAttributes
    ) {
        goalDescriptionDao.update(
            goals.map {
                Pair(
                    it.id,
                    updateAttributes
                )
            }
        )
    }


    val currentGoals = goalInstanceDao.getWithDescriptionsWithLibraryItems()
    val lastFiveCompletedGoals = goalInstanceDao.getLastFiveCompletedWithDescriptionsWithLibraryItems()


    /** Mutators */

    /** Add */
    suspend fun add(
        goalDescriptionCreationAttributes: GoalDescriptionCreationAttributes,
        startingTimeFrame : Calendar = Calendar.getInstance(),
        libraryItems: List<LibraryItem>?,
        target: Int,
    ) = goalDescriptionDao.insert(
        goalDescription = GoalDescriptionModel(
            type = goalDescriptionCreationAttributes.type,
            repeat = goalDescriptionCreationAttributes.repeat,
            periodInPeriodUnits = goalDescriptionCreationAttributes.periodInPeriodUnits,
            periodUnit = goalDescriptionCreationAttributes.periodUnit,
        ),
        startingTimeFrame = startingTimeFrame,
        libraryItemIds = libraryItems?.map { it.id },
        target = target,
    )

    private suspend fun createInstance(
        description: GoalDescription,
        timeFrame: Calendar,
        target: Int,
    ) {
        goalInstanceDao.insert(
            description,
            timeFrame,
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

    suspend fun markInstanceAsRenewed(
        goal: GoalInstance,
    ) {
        goalInstanceDao.update(
            goal.id,
            GoalInstanceUpdateAttributes(renewed = true)
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
            goals,
            GoalDescriptionUpdateAttributes(paused = true)
        )

        // TODO delete current goal instances

    }

    @Transaction
    suspend fun unpause(goal: GoalInstanceWithDescription) {
        unpause(listOf(goal))
    }

    @Transaction
    suspend fun unpause(goals: List<GoalInstanceWithDescription>) {
        update(
            goals.map { it.description },
            GoalDescriptionUpdateAttributes(paused = false)
        )

        // TODO create new goal instances

    }

    /** Archive / Unarchive */

    suspend fun archive(goal: GoalDescription) {
        archive(listOf(goal))
    }

    suspend fun archive(goals: List<GoalDescription>) {
        update(
            goals,
            GoalDescriptionUpdateAttributes(archived = true)
        )
    }

    suspend fun unarchive(goal: GoalDescription) {
        unarchive(listOf(goal))
    }

    suspend fun unarchive(goals: List<GoalDescription>) {
        update(
            goals,
            GoalDescriptionUpdateAttributes(archived = false)
        )
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


    /** Utility functions */

    // Sort
    fun sort(
        goals: List<GoalInstanceWithDescriptionWithLibraryItems>,
        mode: GoalsSortMode,
        direction: SortDirection,
    ) = when (direction) {
        SortDirection.ASCENDING -> {
            when (mode) {
                GoalsSortMode.DATE_ADDED -> goals.sortedBy { it.description.description.createdAt }
                GoalsSortMode.TARGET -> goals.sortedBy { it.instance.target }
                GoalsSortMode.PERIOD -> goals.sortedBy { it.instance.periodInSeconds }
                GoalsSortMode.CUSTOM -> goals // TODO
            }
        }
        SortDirection.DESCENDING -> {
            when (mode) {
                GoalsSortMode.DATE_ADDED -> goals.sortedByDescending { it.description.description.createdAt }
                GoalsSortMode.TARGET -> goals.sortedByDescending { it.instance.target }
                GoalsSortMode.PERIOD -> goals.sortedByDescending { it.instance.periodInSeconds }
                GoalsSortMode.CUSTOM -> goals // TODO()
            }
        }
    }

    /** Update Goals */
    suspend fun updateGoals() {
        while(true) {
            goalInstanceDao.getOutdatedWithDescriptions().let { outdatedInstancesWithDescriptions ->
                if (outdatedInstancesWithDescriptions.isEmpty()) return@updateGoals

                // while there are still outdated goals, keep looping and adding new ones
                outdatedInstancesWithDescriptions.forEach { (outdatedInstance, description) ->
                    if (description.repeat && !description.archived) {

                        // create a new calendar instance, set the time to the instances start timestamp, ...
                        val startCalendar = Calendar.getInstance()
                        startCalendar.timeInMillis = outdatedInstance.startTimestamp * 1000L

                        // ... add to the calendar the period in period units, ...
                        when (description.periodUnit) {
                            GoalPeriodUnit.DAY ->
                                startCalendar.add(
                                    Calendar.DAY_OF_YEAR,
                                    description.periodInPeriodUnits
                                )

                            GoalPeriodUnit.WEEK ->
                                startCalendar.add(
                                    Calendar.WEEK_OF_YEAR,
                                    description.periodInPeriodUnits
                                )

                            GoalPeriodUnit.MONTH ->
                                startCalendar.add(Calendar.MONTH, description.periodInPeriodUnits)
                        }

                        // ... and create a new goal with the same groupId, period and target
                        createInstance(
                            description,
                            startCalendar,
                            outdatedInstance.target
                        )

                        // if the outdated instance belonged to a paused goal delete it...
                        if (description.paused) {
                            goalInstanceDao.delete(outdatedInstance.id)
                        // otherwise mark it as renewed
                        } else {
                            markInstanceAsRenewed(outdatedInstance)
                        }

                    } else if (!description.archived) {
                        // one shot goals are archived after they are outdated
                        archive(description)
                    }
                }
            }
        }
    }
}