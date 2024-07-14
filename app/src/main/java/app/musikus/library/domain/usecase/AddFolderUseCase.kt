/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.library.data.daos.InvalidLibraryFolderException
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.LibraryRepository

class AddFolderUseCase(
    private val libraryRepository: LibraryRepository
) {

    @Throws(InvalidLibraryFolderException::class)
    suspend operator fun invoke(
        creationAttributes: LibraryFolderCreationAttributes
    ) {
        if(creationAttributes.name.isBlank()) {
            throw InvalidLibraryFolderException("Folder name can not be empty")
        }


        libraryRepository.addFolder(
            creationAttributes = creationAttributes
        )
    }
}