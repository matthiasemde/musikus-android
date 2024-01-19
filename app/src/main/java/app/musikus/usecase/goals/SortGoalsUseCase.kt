/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.sorted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SortGoalsUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    operator fun invoke(
        goalsFlow: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
    ) : Flow<List<GoalDescriptionWithInstancesAndLibraryItems>> {
        return combine(
            goalsFlow,
            userPreferencesRepository.goalSortInfo
        ) { goals, (sortMode, sortDirection) ->
            goals.sorted(sortMode as GoalsSortMode, sortDirection)
        }
    }
}