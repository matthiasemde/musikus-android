/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.library.domain.LibraryRepository
import java.util.UUID

class DeleteItemsUseCase(
    private val libraryRepository: LibraryRepository
) {

    suspend operator fun invoke(itemIds : List<UUID>) {
        libraryRepository.deleteItems(itemIds)
    }
}