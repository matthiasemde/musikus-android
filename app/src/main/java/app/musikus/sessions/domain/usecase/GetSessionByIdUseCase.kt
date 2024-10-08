/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.domain.usecase

import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.sessions.domain.SessionRepository
import java.util.UUID

class GetSessionByIdUseCase(
    private val sessionRepository: SessionRepository
) {

    suspend operator fun invoke(sessionId: UUID): SessionWithSectionsWithLibraryItems {
        return sessionRepository.getSession(sessionId)
    }
}
