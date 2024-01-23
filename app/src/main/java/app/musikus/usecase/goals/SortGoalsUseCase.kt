/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.usecase.userpreferences.GetGoalSortInfoUseCase
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.sorted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SortGoalsUseCase(
    private val getGoalSortInfo: GetGoalSortInfoUseCase
) {

    @JvmName("sortGoalDescriptionWithInstances")
    operator fun invoke(
        goalsFlow: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
    ) : Flow<List<GoalDescriptionWithInstancesAndLibraryItems>> {
        return combine(
            goalsFlow,
            getGoalSortInfo()
        ) { goals, (sortMode, sortDirection) ->
            goals.sorted(sortMode as GoalsSortMode, sortDirection)
        }
    }

    @JvmName("sortGoalInstanceWithDescription")
    operator fun invoke(
        goalsFlow: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
    ) : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>> {
        return combine(
            goalsFlow,
            getGoalSortInfo()
        ) { goals, (sortMode, sortDirection) ->
            goals.sorted(sortMode as GoalsSortMode, sortDirection)
        }
    }
}