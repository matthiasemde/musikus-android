/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.repository

import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.database.daos.Section
import app.musikus.database.daos.SectionDao
import app.musikus.database.daos.Session
import app.musikus.database.daos.SessionDao
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SectionUpdateAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.database.entities.SessionUpdateAttributes
import app.musikus.utils.TimeProvider
import app.musikus.utils.Timeframe
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

interface SessionRepository {
    val sessions : Flow<List<Session>>
    val sections : Flow<List<Section>>

    val sessionsWithSectionsWithLibraryItems : Flow<List<SessionWithSectionsWithLibraryItems>>
    suspend fun sessionWithSectionsWithLibraryItems(id: UUID) : SessionWithSectionsWithLibraryItems

    fun sessionsInTimeframe (timeframe: Timeframe) : Flow<List<SessionWithSectionsWithLibraryItems>>

    suspend fun sectionsForSession(sessionId: UUID): List<Section>

    suspend fun sectionsForGoal (goal: GoalInstanceWithDescriptionWithLibraryItems) : Flow<List<Section>>
    suspend fun sectionsForGoal (
        instance: GoalInstance,
        description: GoalDescription,
        libraryItems: List<LibraryItem>
    ) : Flow<List<Section>>

    /** Mutators */
    /** Add */
    suspend fun add(
        sessionCreationAttributes: SessionCreationAttributes,
        sectionCreationAttributes: List<SectionCreationAttributes>
    ) : Pair<UUID, List<UUID>>

    /** Update */

    suspend fun updateSession(
        id: UUID,
        sessionUpdateAttributes: SessionUpdateAttributes,
    )

    suspend fun updateSections(
        updateData: List<Pair<UUID, SectionUpdateAttributes>>
    )

    /** Delete / Restore */
    suspend fun delete(sessionIds: List<UUID>)
    suspend fun restore(sessionIds: List<UUID>)

    /** Exists */
    suspend fun existsSession(id: UUID) : Boolean

    /** Clean */
    suspend fun clean()

    /** Transaction */
    suspend fun withTransaction(block: suspend () -> Unit)
}

class SessionRepositoryImpl(
    private val timeProvider: TimeProvider,
    private val sessionDao : SessionDao,
    private val sectionDao : SectionDao,
    private val withDatabaseTransaction: suspend (suspend () -> Unit) -> Unit,
) : SessionRepository {


    /** Accessors */
    override val sessions = sessionDao.getAllAsFlow()
    override val sections = sectionDao.getAllAsFlow()

    override val sessionsWithSectionsWithLibraryItems = sessionDao.getAllWithSectionsWithLibraryItemsAsFlow()
    override suspend fun sessionWithSectionsWithLibraryItems(
        id: UUID
    ): SessionWithSectionsWithLibraryItems {
        return sessionDao.getWithSectionsWithLibraryItems(id)
    }

    override fun sessionsInTimeframe (
        timeframe: Timeframe
    ) : Flow<List<SessionWithSectionsWithLibraryItems>> {
        return sessionDao.getFromTimeframe(timeframe)
    }

    override suspend fun sectionsForSession(sessionId: UUID): List<Section> {
        return sectionDao.getForSession(sessionId)
    }

    private suspend fun sectionsForGoal (
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
        itemIds: List<UUID>? = null
    ) = if (itemIds == null) sectionDao.getInTimeframe(
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
    ) else sectionDao.getInTimeframeForItemId(
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        itemIds = itemIds
    )

    override suspend fun sectionsForGoal (goal: GoalInstanceWithDescriptionWithLibraryItems) = sectionsForGoal(
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