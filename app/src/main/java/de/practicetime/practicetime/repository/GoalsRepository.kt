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
import de.practicetime.practicetime.datastore.GoalsSortMode
import de.practicetime.practicetime.datastore.SortDirection
import java.util.*

class GoalsRepository(
    database: PTDatabase
) {
    private val goalInstanceDao = database.goalInstanceDao
    private val goalDescriptionDao = database.goalDescriptionDao

    val goals = goalInstanceDao.getWithDescriptionsWithLibraryItems()

    /** Mutators */
    /** Add */
    suspend fun addGoal(
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
    suspend fun archiveGoals(goalDescriptionIds: List<UUID>) {
        goalDescriptionDao.getAndArchive(goalDescriptionIds)
    }

    /** Sort */
    fun sortGoals(
        goals: List<GoalInstanceWithDescriptionWithLibraryItems>,
        mode: GoalsSortMode,
        direction: SortDirection,
    ): List<GoalInstanceWithDescriptionWithLibraryItems> {
//        if(mode != null) {
//            if (mode == goalsSortMode.value) {
//                when (goalsSortDirection.value) {
//                    SortDirection.ASCENDING -> goalsSortDirection.value = SortDirection.DESCENDING
//                    SortDirection.DESCENDING -> goalsSortDirection.value = SortDirection.ASCENDING
//                }
//            } else {
//                goalsSortDirection.value = SortDirection.ASCENDING
//                goalsSortMode.value = mode
//                PracticeTime.prefs.edit().putString(
//                    PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_MODE,
//                    goalsSortMode.value.name
//                ).apply()
//            }
//            PracticeTime.prefs.edit().putString(
//                PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_DIRECTION,
//                goalsSortDirection.value.name
//            ).apply()
//        }
        return when (direction) {
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
}