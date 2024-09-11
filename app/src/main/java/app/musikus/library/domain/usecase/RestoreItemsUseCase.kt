/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.library.domain.LibraryRepository
import java.util.UUID

class RestoreItemsUseCase(
    private val libraryRepository: LibraryRepository
) {

    suspend operator fun invoke(itemIds: List<UUID>) {
        libraryRepository.restoreItems(itemIds)
    }
}
