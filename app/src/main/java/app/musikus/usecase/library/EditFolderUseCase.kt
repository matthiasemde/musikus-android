/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.daos.InvalidLibraryFolderException
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.repository.LibraryRepository
import java.util.UUID

class EditFolderUseCase(
    private val libraryRepository: LibraryRepository
) {

    @Throws(InvalidLibraryFolderException::class)
    suspend operator fun invoke(
        id: UUID,
        updateAttributes: LibraryFolderUpdateAttributes
    ) {
        if(updateAttributes.name != null && updateAttributes.name.isBlank()) {
            throw InvalidLibraryFolderException("Folder name can not be empty")
        }

        libraryRepository.editFolder(
            id = id,
            updateAttributes = updateAttributes
        )
    }
}