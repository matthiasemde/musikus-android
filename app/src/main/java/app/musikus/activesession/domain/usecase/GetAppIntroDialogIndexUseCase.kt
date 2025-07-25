/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Michael Prommersberger
 */

package app.musikus.activesession.domain.usecase

import app.musikus.core.domain.AppIntroDialogScreens
import app.musikus.core.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAppIntroDialogIndexUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    operator fun invoke(): Flow<Int> {
        return userPreferencesRepository.appIntroDialogIndices.map {
            it[AppIntroDialogScreens.ACTIVESESSION] ?: 0    // return 0 if no index is set
        }
    }
}