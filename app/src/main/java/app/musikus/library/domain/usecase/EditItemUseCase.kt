/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.library.data.entities.InvalidLibraryItemException
import app.musikus.library.data.entities.LibraryItemUpdateAttributes
import app.musikus.library.domain.LibraryRepository
import java.util.UUID

class EditItemUseCase(
    private val libraryRepository: LibraryRepository
) {

    @Throws(InvalidLibraryItemException::class)
    suspend operator fun invoke(
        id : UUID,
        updateAttributes: LibraryItemUpdateAttributes
    ) {
        if(!libraryRepository.existsItem(id)) {
            throw InvalidLibraryItemException("Item not found")
        }

        if(updateAttributes.name != null && updateAttributes.name.isBlank()) {
            throw InvalidLibraryItemException("Item name cannot be empty")
        }

        if(updateAttributes.colorIndex != null && updateAttributes.colorIndex !in 0..9) {
            throw InvalidLibraryItemException("Color index must be between 0 and 9")
        }

        if(
            updateAttributes.libraryFolderId?.value != null &&
            !libraryRepository.existsFolder(updateAttributes.libraryFolderId.value)
        ) {
            throw InvalidLibraryItemException("Folder (${updateAttributes.libraryFolderId.value}) does not exist")
        }

        libraryRepository.editItem(
            id = id,
            updateAttributes = updateAttributes
        )
    }
}