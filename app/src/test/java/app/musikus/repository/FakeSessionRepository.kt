/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.repository

import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.SectionWithLibraryItem
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.database.daos.Section
import app.musikus.database.daos.Session
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.TimeProvider
import app.musikus.utils.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeSessionRepository(
    private val fakeLibraryRepository: FakeLibraryRepository,
    private val timeProvider: TimeProvider,
    private val idProvider: FakeIdProvider
) : SessionRepository {

    private val _sessions = mutableListOf<SessionWithSectionsWithLibraryItems>()

    override val sessions: Flow<List<Session>>
        get() = TODO("Not yet implemented")
    override val sections: Flow<List<Section>>
        get() = TODO("Not yet implemented")
    override val sessionsWithSectionsWithLibraryItems: Flow<List<SessionWithSectionsWithLibraryItems>>
        get() = flowOf(_sessions)

    override suspend fun sessionWithSectionsWithLibraryItems(id: UUID): SessionWithSectionsWithLibraryItems {
        TODO("Not yet implemented")
    }

    override fun sessionsInTimeframe(timeframe: Timeframe): Flow<List<SessionWithSectionsWithLibraryItems>> {
        TODO("Not yet implemented")
    }

    override suspend fun sectionsForGoal(goal: GoalInstanceWithDescriptionWithLibraryItems): Flow<List<Section>> {
        TODO("Not yet implemented")
    }

    override suspend fun sectionsForGoal(
        instance: GoalInstance,
        description: GoalDescription,
        libraryItems: List<LibraryItem>
    ): Flow<List<Section>> {
        TODO("Not yet implemented")
    }

    override suspend fun add(
        sessionCreationAttributes: SessionCreationAttributes,
        sectionsCreationAttributes: List<SectionCreationAttributes>
    ): Pair<UUID, List<UUID>> {
        val sessionId = idProvider.generateId()
        val sectionIds = sectionsCreationAttributes.map { idProvider.generateId() }

        val libraryItems = fakeLibraryRepository.items.first()

        _sessions.add(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = sessionId,
                    createdAt = timeProvider.now(),
                    modifiedAt = timeProvider.now(),
                    breakDurationSeconds = sessionCreationAttributes.breakDuration.inWholeSeconds,
                    rating = sessionCreationAttributes.rating,
                    comment = sessionCreationAttributes.comment
                ),
                sections = sectionsCreationAttributes
                    .zip(sectionIds)
                    .map { (sectionCreationAttributes, id) ->
                    SectionWithLibraryItem(
                        section = Section(
                            id = id,
                            startTimestamp = sectionCreationAttributes.startTimestamp,
                            durationSeconds = sectionCreationAttributes.duration.inWholeSeconds,
                            libraryItemId = sectionCreationAttributes.libraryItemId,
                            sessionId = sessionId
                        ),
                        libraryItem = libraryItems.first {
                            it.id == sectionCreationAttributes.libraryItemId
                        }
                    )
                },
            )
        )

        return Pair(sessionId, sectionIds)
    }

    override suspend fun delete(sessions: List<Session>) {
        TODO("Not yet implemented")
    }

    override suspend fun restore(sessions: List<Session>) {
        TODO("Not yet implemented")
    }

    override suspend fun clean() {
        TODO("Not yet implemented")
    }

}