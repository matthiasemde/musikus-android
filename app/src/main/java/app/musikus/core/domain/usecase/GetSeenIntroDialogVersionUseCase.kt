/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Michael Prommersberger
 */

package app.musikus.core.domain.usecase

import app.musikus.core.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.map

class  GetSeenIntroDialogVersionUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    operator fun invoke(featureName: String) = userPreferencesRepository.appIntroSeenDialogVersions.map {
        it[featureName] ?: -1    // return -1 if not seen at all
    }
}