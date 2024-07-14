/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.data

import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.core.data.SectionWithLibraryItem
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.library.data.daos.LibraryItem
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.Timeframe
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.sessions.data.daos.Section
import app.musikus.sessions.data.daos.Session
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SectionUpdateAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import app.musikus.sessions.data.entities.SessionUpdateAttributes
import app.musikus.sessions.domain.SessionRepository
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
        get() = throw NotImplementedError()
    override val sections: Flow<List<Section>>
        get() = flowOf(_sessions.flatMap { session -> session.sections.map { it.section } })
    override val orderedSessionsWithSectionsWithLibraryItems: Flow<List<SessionWithSectionsWithLibraryItems>>
        get() = flowOf(_sessions.sortedByDescending { it.startTimestamp })

    override suspend fun getSession(id: UUID): SessionWithSectionsWithLibraryItems {
        throw NotImplementedError()
    }

    override fun sessionsInTimeframe(timeframe: Timeframe): Flow<List<SessionWithSectionsWithLibraryItems>> {
        return flowOf(_sessions.filter { session ->
            session.startTimestamp >= timeframe.first &&
            session.startTimestamp < timeframe.second
        })
    }

    override suspend fun sectionsForSession(sessionId: UUID): List<Section> {
        return _sessions.first { it.session.id == sessionId }.sections.map { it.section }
    }

    override suspend fun sectionsForGoal(goal: GoalInstanceWithDescriptionWithLibraryItems): Flow<List<Section>> {
        throw NotImplementedError()
    }

    override suspend fun sectionsForGoal(
        instance: GoalInstance,
        description: GoalDescription,
        libraryItems: List<LibraryItem>
    ): Flow<List<Section>> {
        throw NotImplementedError()
    }

    override fun getLastSectionsForItems(items: List<LibraryItem>): Flow<List<Section>> {
        throw NotImplementedError()
    }

    override suspend fun add(
        sessionCreationAttributes: SessionCreationAttributes,
        sectionCreationAttributes: List<SectionCreationAttributes>
    ): Pair<UUID, List<UUID>> {
        val sessionId = idProvider.generateId()
        val sectionIds = sectionCreationAttributes.map { idProvider.generateId() }

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
                sections = sectionCreationAttributes
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

    override suspend fun updateSession(id: UUID, sessionUpdateAttributes: SessionUpdateAttributes) {
        val oldSession = _sessions.first { it.session.id == id }
        _sessions.remove(oldSession)
        _sessions.add(
            oldSession.copy(
                session = oldSession.session.copy(
                    modifiedAt = timeProvider.now(),
                    rating = sessionUpdateAttributes.rating ?: oldSession.session.rating,
                    comment = sessionUpdateAttributes.comment ?: oldSession.session.comment
                )
            )
        )
    }

    override suspend fun updateSections(updateData: List<Pair<UUID, SectionUpdateAttributes>>) {
        val oldSession = _sessions.first { sessionWithSections ->
            sessionWithSections.sections.any { section -> section.section.id in updateData.map { it.first } }
        }

        _sessions.remove(oldSession)

        _sessions.add(
            oldSession.copy(
                sections = oldSession.sections.map { oldSection ->
                    updateData.firstOrNull { it.first == oldSection.section.id }?.let {
                        oldSection.copy(
                            section = oldSection.section.copy(
                                durationSeconds = it.second.duration?.inWholeSeconds ?: oldSection.section.durationSeconds,
                            )
                        )

                    } ?: oldSection
                }
            )
        )
    }

    override suspend fun delete(sessionIds: List<UUID>) {
        throw NotImplementedError()
    }

    override suspend fun restore(sessionIds: List<UUID>) {
        throw NotImplementedError()
    }

    override suspend fun existsSession(id: UUID): Boolean {
        return _sessions.any { it.session.id == id }
    }

    override suspend fun clean() {
        throw NotImplementedError()
    }

    override suspend fun withTransaction(block: suspend () -> Unit) {
        block()
    }

}