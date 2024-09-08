/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.goals.data.GoalsSortMode
import app.musikus.goals.data.sorted
import app.musikus.settings.domain.usecase.GetGoalSortInfoUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SortGoalsUseCase(
    private val getGoalSortInfo: GetGoalSortInfoUseCase
) {

    @JvmName("sortGoalDescriptionWithInstances")
    operator fun invoke(
        goalsFlow: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
    ): Flow<List<GoalDescriptionWithInstancesAndLibraryItems>> {
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
    ): Flow<List<GoalInstanceWithDescriptionWithLibraryItems>> {
        return combine(
            goalsFlow,
            getGoalSortInfo()
        ) { goals, (sortMode, sortDirection) ->
            goals.sorted(sortMode as GoalsSortMode, sortDirection)
        }
    }
}
