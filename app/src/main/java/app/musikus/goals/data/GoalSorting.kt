/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.data

import app.musikus.R
import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.core.domain.SortMode
import app.musikus.core.presentation.utils.UiText
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance

typealias GoalSortInfo = SortInfo<Pair<GoalDescription, GoalInstance>>

enum class GoalsSortMode : SortMode<Pair<GoalDescription, GoalInstance>> {
    DATE_ADDED {
        override val label = UiText.StringResource(R.string.goals_goal_sort_mode_date_added)
        override val comparator = compareBy<Pair<GoalDescription, GoalInstance>> { (description, _) ->
            description.createdAt
        }
    },
    TARGET {
        override val label = UiText.StringResource(R.string.goals_goal_sort_mode_target)
        override val comparator = compareBy<Pair<GoalDescription, GoalInstance>> { (_, instance) ->
            instance.target
        }
    },
    PERIOD {
        override val label = UiText.StringResource(R.string.goals_goal_sort_mode_period)
        override val comparator = compareBy<Pair<GoalDescription, GoalInstance>> { (description, _) ->
            description.periodUnit
        }.thenBy { (description, _) ->
            description.periodInPeriodUnits
        }
    },
//    CUSTOM {
//        override val label = "Custom"
//        override val comparator = compareBy<Pair<GoalDescription, GoalInstance>> { TODO }
//    }
    ;

    override val isDefault: Boolean
        get() = this == DEFAULT

    companion object {
        val DEFAULT = DATE_ADDED

        fun valueOrDefault(string: String?) = try {
            valueOf(string ?: "")
        } catch (e: Exception) {
            DEFAULT
        }
    }
}

@JvmName("sortedGoalInstanceWithDescriptionWithLibraryItems")
fun List<GoalInstanceWithDescriptionWithLibraryItems>.sorted(
    mode: GoalsSortMode,
    direction: SortDirection
) : List<GoalInstanceWithDescriptionWithLibraryItems> = this.sortedWith(
    when(direction) {
        SortDirection.ASCENDING ->
            compareBy (mode.comparator) { it.description.description to it.instance}
        SortDirection.DESCENDING ->
            compareByDescending(mode.comparator) { it.description.description to it.instance }
    }
)

@JvmName("sortedGoalDescriptionWithInstancesAndLibraryItems")
fun List<GoalDescriptionWithInstancesAndLibraryItems>.sorted(
    mode: GoalsSortMode,
    direction: SortDirection
) : List<GoalDescriptionWithInstancesAndLibraryItems> = this.sortedWith(
    when(direction) {
        SortDirection.ASCENDING ->
            compareBy (mode.comparator) { it.description to it.latestInstance }
        SortDirection.DESCENDING ->
            compareByDescending(mode.comparator) { it.description to it.latestInstance }
    }
)
