/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.repository.LibraryRepository
import app.musikus.repository.UserPreferencesRepository

class GetItemsUseCase(
    private val libraryRepository: LibraryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {

//    operator fun invoke(
//        folderId: Nullable<UUID> = null,
//    ): Flow<List<LibraryItem>> {
//        val items = libraryRepository.items.first()
//        val folders = libraryRepository.folders.first()
//
//        val showHidden = userPreferencesRepository.showHiddenItems.first()
//
//        return items
//            .filter { showHidden || !it.hidden }
//            .map { item ->
//                val folder = folders.find { it.id == item.folderId }
//                item.copy(folder = folder)
//            }
//    }
}
