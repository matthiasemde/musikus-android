/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.daos.LibraryItem
import app.musikus.repository.LibraryRepository

class DeleteItemsUseCase(
    private val libraryRepository: LibraryRepository
) {

    suspend operator fun invoke(items : List<LibraryItem>) {
        libraryRepository.deleteItems(items.map { it.id })
    }
}