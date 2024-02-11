/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.repository.LibraryRepository
import java.util.UUID

class DeleteFoldersUseCase(
    private val libraryRepository: LibraryRepository
) {

    suspend operator fun invoke(folderIds : List<UUID>) {
        libraryRepository.deleteFolders(folderIds)
    }
}