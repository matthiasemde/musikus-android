/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.userpreferences

import app.musikus.repository.UserPreferencesRepository

class GetThemeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    operator fun invoke() = userPreferencesRepository.theme
}