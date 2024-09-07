/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.core.data.LibraryFolderWithItems
import app.musikus.library.data.LibraryFolderSortMode
import app.musikus.library.data.sorted
import app.musikus.library.domain.LibraryRepository
import app.musikus.settings.domain.usecase.GetFolderSortInfoUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetSortedLibraryFoldersUseCase(
    private val libraryRepository: LibraryRepository,
    private val getFolderSortInfo: GetFolderSortInfoUseCase,
) {

    operator fun invoke() : Flow<List<LibraryFolderWithItems>> {
        return combine(
            libraryRepository.folders,
            getFolderSortInfo()
        ) { folders, folderSortInfo ->
            folders.sorted(
                folderSortInfo.mode as LibraryFolderSortMode,
                folderSortInfo.direction
            )
        }
    }
}