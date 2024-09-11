/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.goals.domain.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetAllGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val sortGoals: SortGoalsUseCase
) {

    operator fun invoke(): Flow<List<GoalDescriptionWithInstancesAndLibraryItems>> {
        return sortGoals(goalRepository.allGoals)
    }
}
