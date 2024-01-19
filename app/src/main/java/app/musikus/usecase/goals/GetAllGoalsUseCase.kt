/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetAllGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val sortGoals: SortGoalsUseCase
) {

    operator fun invoke() : Flow<List<GoalDescriptionWithInstancesAndLibraryItems>> {
        return sortGoals(goalRepository.allGoals)
    }
}
