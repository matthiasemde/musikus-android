/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.repository

import androidx.room.Transaction
import app.musikus.database.GoalDescriptionWithLibraryItems
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.PTDatabase
import app.musikus.database.entities.GoalDescription
import app.musikus.datastore.GoalsSortMode
import app.musikus.datastore.SortDirection
import app.musikus.utils.getCurrTimestamp
import java.util.Calendar
import java.util.UUID

class GoalRepository(
    database: PTDatabase
) {
    private val goalInstanceDao = database.goalInstanceDao
    private val goalDescriptionDao = database.goalDescriptionDao

    val currentGoals = goalInstanceDao.getWithDescriptionsWithLibraryItems()

    /** Mutators */

    /** Add */
    suspend fun add(
        newGoal: GoalDescriptionWithLibraryItems,
        target: Int,
    ) {
        goalDescriptionDao.insert(
            newGoal,
            target,
        )
    }

    /** Edit */
    suspend fun editGoalTarget(
        editedGoalDescriptionId: UUID,
        newTarget: Int,
    ) {
        goalInstanceDao.apply {
            get(
                goalDescriptionId = editedGoalDescriptionId,
                from = getCurrTimestamp(),
            ).forEach {
                it.target = newTarget
                update(it)
            }
        }
    }

    /** Pause/Unpause */
    @Transaction
    suspend fun pause(goal: GoalInstanceWithDescriptionWithLibraryItems) {
        val description = goal.description.description

        description.paused = true
        goalDescriptionDao.update(description)
    }

    @Transaction
    suspend fun pause(goals: List<GoalInstanceWithDescriptionWithLibraryItems>) {
        goals.forEach { pause(it) }
    }

    @Transaction
    suspend fun unpause(pausedGoal: GoalInstanceWithDescriptionWithLibraryItems) {
        val (instance, descriptionWithLibraryItems) = pausedGoal
        val description = descriptionWithLibraryItems.description

        description.paused = false
        goalDescriptionDao.update(description)
        if(instance.startTimestamp + instance.periodInSeconds > getCurrTimestamp()) {
            instance.renewed = false
            goalInstanceDao.update(instance)
        } else {
            goalInstanceDao.insert(
                description.createInstance(Calendar.getInstance(), instance.target)
            )
        }
    }

    @Transaction
    suspend fun unpause(pausedGoals: List<GoalInstanceWithDescriptionWithLibraryItems>) {
        pausedGoals.forEach { unpause(it) }
    }

    /** Archive */

    suspend fun archive(goalDescription: GoalDescription) {
        goalDescription.archived = true
        goalDescriptionDao.update(goalDescription)
    }

    suspend fun archive(goalDescriptions: List<GoalDescription>) {
        goalDescriptions.forEach { archive(it) }
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
}