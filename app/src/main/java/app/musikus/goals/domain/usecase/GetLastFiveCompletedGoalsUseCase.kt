/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.goals.domain.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.goals.domain.GoalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class GetLastFiveCompletedGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val calculateProgress: CalculateGoalProgressUseCase
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<GoalInstanceWithProgressAndDescriptionWithLibraryItems>> {
        return goalRepository.lastFiveCompletedGoals.flatMapLatest { goals ->
            calculateProgress(goals).map { progress ->
                goals.zip(progress).map { (goal, progress) ->
                    GoalInstanceWithProgressAndDescriptionWithLibraryItems(
                        description = goal.description,
                        instance = goal.instance,
                        progress = progress,
                    )
                }
            }
        }
    }
}
