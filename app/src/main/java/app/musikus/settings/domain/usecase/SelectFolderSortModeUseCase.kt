/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.settings.domain.usecase

import app.musikus.settings.data.UserPreferencesRepository
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import kotlinx.coroutines.flow.first

class SelectFolderSortModeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend operator fun invoke(sortMode: LibraryFolderSortMode) {
        val currentSortInfo = userPreferencesRepository.folderSortInfo.first()

        if (currentSortInfo.mode == sortMode) {
            userPreferencesRepository.updateLibraryFolderSortInfo(currentSortInfo.copy(
                direction = currentSortInfo.direction.invert()
            ))
            return
        }
        userPreferencesRepository.updateLibraryFolderSortInfo(SortInfo(
            mode = sortMode,
            direction = SortDirection.DEFAULT
        ))
    }
}