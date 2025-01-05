/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Matthias Emde
 */

package app.musikus.core.domain.usecase

import app.musikus.core.domain.UserPreferencesRepository
import app.musikus.core.presentation.CURRENT_ANNOUNCEMENT_ID

class ResetAnnouncementMessageUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend operator fun invoke() {
        userPreferencesRepository.updateIdOfLastAnnouncementSeen(CURRENT_ANNOUNCEMENT_ID - 1)
    }
}
