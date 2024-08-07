/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.settings.domain.usecase

import app.musikus.settings.domain.UserPreferencesRepository
import app.musikus.core.domain.LibraryItemSortMode
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import kotlinx.coroutines.flow.first

class SelectItemSortModeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend operator fun invoke(sortMode: LibraryItemSortMode) {
        val currentSortInfo = userPreferencesRepository.itemSortInfo.first()

        if (currentSortInfo.mode == sortMode) {
            userPreferencesRepository.updateLibraryItemSortInfo(currentSortInfo.copy(
                direction = currentSortInfo.direction.invert()
            ))
            return
        }
        userPreferencesRepository.updateLibraryItemSortInfo(
            SortInfo(
            mode = sortMode,
            direction = SortDirection.DEFAULT
        )
        )
    }
}