/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.repository

import de.practicetime.practicetime.database.GoalDescriptionWithLibraryItems
import de.practicetime.practicetime.database.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.GoalDescription
import de.practicetime.practicetime.datastore.GoalsSortMode
import de.practicetime.practicetime.datastore.SortDirection
import java.util.*

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
        goalDescriptionDao.updateTarget(editedGoalDescriptionId, newTarget)
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