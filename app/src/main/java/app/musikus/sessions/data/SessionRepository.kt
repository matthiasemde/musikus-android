/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.sessions.data

import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.Timeframe
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.library.data.daos.LibraryItem
import app.musikus.sessions.data.daos.Section
import app.musikus.sessions.data.daos.SectionDao
import app.musikus.sessions.data.daos.SessionDao
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SectionUpdateAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import app.musikus.sessions.data.entities.SessionUpdateAttributes
import app.musikus.sessions.domain.SessionRepository
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

class SessionRepositoryImpl(
    private val timeProvider: TimeProvider,
    private val sessionDao: SessionDao,
    private val sectionDao: SectionDao,
    private val withDatabaseTransaction: suspend (suspend () -> Unit) -> Unit,
) : SessionRepository {

    /** Accessors */
    override val sessions = sessionDao.getAllAsFlow()
    override val sections = sectionDao.getAllAsFlow()

    override val sessionsWithSectionsWithLibraryItems =
        sessionDao.getAllWithSectionsWithLibraryItems()
    override val orderedSessionsWithSectionsWithLibraryItems = sessionDao.getOrderedWithSectionsWithLibraryItems()
    override suspend fun getSession(
        id: UUID
    ): SessionWithSectionsWithLibraryItems {
        return sessionDao.getWithSectionsWithLibraryItems(id)
    }

    override fun sessionsInTimeframe(
        timeframe: Timeframe
    ): Flow<List<SessionWithSectionsWithLibraryItems>> {
        return sessionDao.getFromTimeframe(timeframe)
    }

    override suspend fun sectionsForSession(sessionId: UUID): List<Section> {
        return sectionDao.getForSession(sessionId)
    }

    private suspend fun sectionsForGoal(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
        itemIds: List<UUID>? = null
    ) = if (itemIds == null) {
        sectionDao.getInTimeframe(
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
        )
    } else {
        sectionDao.getInTimeframeForItemId(
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            itemIds = itemIds
        )
    }

    override suspend fun sectionsForGoal(goal: GoalInstanceWithDescriptionWithLibraryItems) = sectionsForGoal(
        startTimestamp = goal.instance.startTimestamp,
        endTimestamp = goal.endTimestampInLocalTimezone(timeProvider),
        itemIds = goal.description.libraryItems.map { it.id }.takeIf { it.isNotEmpty() }
    )

    override suspend fun sectionsForGoal(
        instance: GoalInstance,
        description: GoalDescription,
        libraryItems: List<LibraryItem>
    ) = sectionsForGoal(
        startTimestamp = instance.startTimestamp,
        endTimestamp = description.endOfInstanceInLocalTimezone(instance, timeProvider),
        itemIds = libraryItems.map { it.id }.takeIf { it.isNotEmpty() }
    )

    override fun getLastSectionsForItems(items: List<LibraryItem>): Flow<List<Section>> {
        return sectionDao.getLatestForItems(items.map { it.id })
    }

    /** Mutators */
    /** Add */
    override suspend fun add(
        sessionCreationAttributes: SessionCreationAttributes,
        sectionCreationAttributes: List<SectionCreationAttributes>
    ): Pair<UUID, List<UUID>> {
        return sessionDao.insert(
            sessionCreationAttributes,
            sectionCreationAttributes
        )
    }

    /** Update */

    override suspend fun updateSession(
        id: UUID,
        sessionUpdateAttributes: SessionUpdateAttributes,
    ) {
        sessionDao.update(id, sessionUpdateAttributes)
    }

    override suspend fun updateSections(
        updateData: List<Pair<UUID, SectionUpdateAttributes>>,
    ) {
        sectionDao.update(updateData)
    }

    /** Delete / Restore */
    override suspend fun delete(sessionIds: List<UUID>) {
        sessionDao.delete(sessionIds)
    }

    override suspend fun restore(sessionIds: List<UUID>) {
        sessionDao.restore(sessionIds)
    }

    override suspend fun existsSession(id: UUID): Boolean {
        return sessionDao.exists(id)
    }

    /** Clean */
    override suspend fun clean() {
        sessionDao.clean()
    }

    /** Transaction */

    override suspend fun withTransaction(block: suspend () -> Unit) {
        withDatabaseTransaction(block)
    }
}
