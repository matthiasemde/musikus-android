/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.library.data.daos.LibraryItem
import app.musikus.sessionslist.data.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZonedDateTime
import java.util.UUID

class GetLastPracticedDateUseCase(
    private val sessionRepository: SessionRepository
) {

    operator fun invoke(items: List<LibraryItem>) : Flow<Map<UUID, ZonedDateTime>> {
        return sessionRepository.getLastSectionsForItems(items).map { sections ->
            sections.groupBy { it.libraryItemId }.mapValues { (_, sections) ->
                sections.single().startTimestamp
            }
        }
    }
}