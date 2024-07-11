/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.settings.domain.usecase

import app.musikus.settings.data.UserPreferencesRepository

class GetFolderSortInfoUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    operator fun invoke() = userPreferencesRepository.folderSortInfo
}