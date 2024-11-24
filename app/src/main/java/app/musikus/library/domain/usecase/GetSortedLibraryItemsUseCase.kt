/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.core.data.Nullable
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.sorted
import app.musikus.library.domain.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class GetSortedLibraryItemsUseCase(
    private val libraryRepository: LibraryRepository,
    private val getItemSortInfo: GetItemSortInfoUseCase,
) {

    /**
     * Get a sorted list library items.
     *
     * @param folderId: If null, all items are returned.
     * Otherwise only items of the given folder are returned.
     * A value of [Nullable(null)] refers to the root folder.
     * */
    operator fun invoke(
        folderId: Nullable<UUID>? = null,
    ): Flow<List<LibraryItem>> {
        return libraryRepository.items
            .map { items ->
                if (folderId == null) {
                    items
                } else {
                    items.filter { item ->
                        item.libraryFolderId == folderId.value
                    }
                }
            }
            .combine(
                getItemSortInfo()
            ) { filteredItems, itemSortInfo ->
                filteredItems.sorted(
                    itemSortInfo.mode as LibraryItemSortMode,
                    itemSortInfo.direction
                )
            }
    }
}
