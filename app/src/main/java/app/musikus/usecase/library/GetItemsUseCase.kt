/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.Nullable
import app.musikus.database.daos.LibraryItem
import app.musikus.repository.LibraryRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.sorted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class GetItemsUseCase(
    private val libraryRepository: LibraryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
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
                if (folderId == null) items
                else items.filter { item ->
                    item.libraryFolderId == folderId.value
                }
            }
            .combine(
                userPreferencesRepository.userPreferences
            ) { filteredItems, preferences ->
                filteredItems.sorted(
                    preferences.libraryItemSortMode,
                    preferences.libraryItemSortDirection
                )
            }
    }
}
