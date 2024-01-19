/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetCurrentGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val sortGoalsUseCase: SortGoalsUseCase
) {

    operator fun invoke(
        showPaused: Boolean
    ) : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>> {
        return sortGoalsUseCase(goalRepository.currentGoals).map { goals ->
            if (showPaused) {
                goals
            } else {
                goals.filter { !it.description.description.paused }
            }
        }
    }
}