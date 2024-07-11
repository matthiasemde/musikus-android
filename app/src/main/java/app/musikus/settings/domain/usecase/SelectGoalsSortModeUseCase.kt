/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.settings.domain.usecase

import app.musikus.settings.data.UserPreferencesRepository
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import kotlinx.coroutines.flow.first

class SelectGoalsSortModeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend operator fun invoke(sortMode: GoalsSortMode) {
        val currentSortInfo = userPreferencesRepository.goalSortInfo.first()

        if (currentSortInfo.mode == sortMode) {
            userPreferencesRepository.updateGoalSortInfo(currentSortInfo.copy(
                direction = currentSortInfo.direction.invert()
            ))
            return
        }
        userPreferencesRepository.updateGoalSortInfo(SortInfo(
            mode = sortMode,
            direction = SortDirection.DEFAULT
        ))
    }
}