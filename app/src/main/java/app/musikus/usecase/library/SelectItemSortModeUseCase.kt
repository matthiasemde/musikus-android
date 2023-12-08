/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.daos.LibraryItem
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import app.musikus.utils.SortMode
import kotlinx.coroutines.flow.first

class SelectItemSortModeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend operator fun invoke(sortMode: SortMode<LibraryItem>) {
        val currentSortInfo = userPreferencesRepository.itemSortInfo.first()

        if (currentSortInfo.mode == sortMode) {
            userPreferencesRepository.updateLibraryItemSortInfo(currentSortInfo.copy(
                direction = currentSortInfo.direction.invert()
            ))
            return
        }
        userPreferencesRepository.updateLibraryItemSortInfo(SortInfo(
            mode = sortMode,
            direction = SortDirection.DEFAULT
        ))
    }
}