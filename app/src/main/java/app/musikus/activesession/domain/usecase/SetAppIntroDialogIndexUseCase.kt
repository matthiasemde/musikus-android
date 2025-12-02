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

class SetAppIntroDialogIndexUseCase (
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend operator fun invoke(index: Int) {
        userPreferencesRepository.updateAppIntroDialogIndex(
            screen = AppIntroDialogScreens.ACTIVESESSION,
            index = index
        )
    }
}