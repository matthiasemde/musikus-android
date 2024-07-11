/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.database.entities.InvalidLibraryItemException
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.library.data.LibraryRepository

class AddItemUseCase(
    private val libraryRepository: LibraryRepository
) {

    @Throws(InvalidLibraryItemException::class)
    suspend operator fun invoke(
        creationAttributes: LibraryItemCreationAttributes
    ) {
        if(creationAttributes.name.isBlank()) {
            throw InvalidLibraryItemException("Item name cannot be empty")
        }

        if(creationAttributes.colorIndex !in 0..9) {
            throw InvalidLibraryItemException("Color index must be between 0 and 9")
        }

        if(
            creationAttributes.libraryFolderId.value != null &&
            !libraryRepository.existsFolder(creationAttributes.libraryFolderId.value)
        ) {
            throw InvalidLibraryItemException("Folder (${creationAttributes.libraryFolderId.value}) does not exist")
        }

        libraryRepository.addItem(
            creationAttributes = creationAttributes
        )
    }
}