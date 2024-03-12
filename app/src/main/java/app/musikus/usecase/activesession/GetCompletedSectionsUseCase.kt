/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.usecase.activesession

import kotlinx.coroutines.flow.map

class GetCompletedSectionsUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    operator fun invoke() = activeSessionRepository.getSessionState().map {
        it?.completedSections ?: emptyList()
    }
}