/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.activesession.domain.usecase

import app.musikus.activesession.domain.ActiveSessionRepository

class GetActiveSessionStateUseCase(
    private val activeSessionRepository: ActiveSessionRepository
) {
    operator fun invoke() = activeSessionRepository.getSessionState()
}
