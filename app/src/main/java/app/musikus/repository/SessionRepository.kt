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
import app.musikus.database.entities.SessionCreationAttributes
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
        sectionsCreationAttributes: List<SectionCreationAttributes>
    ) : Pair<UUID, List<UUID>>

    /** Delete / Restore */
    suspend fun delete(sessions: List<Session>)
    suspend fun restore(sessions: List<Session>)

    /** Clean */
    suspend fun clean()
}

class SessionRepositoryImpl(
    private val timeProvider: TimeProvider,
    private val sessionDao : SessionDao,
    private val sectionDao : SectionDao,
) : SessionRepository {


    /** Accessors */
    override val sessions = sessionDao.getAllAsFlow()
    override val sections = sectionDao.getAllAsFlow()

    override val sessionsWithSectionsWithLibraryItems = sessionDao.getAllWithSectionsWithLibraryItemsAsFlow()
    override suspend fun sessionWithSectionsWithLibraryItems(id: UUID) = sessionDao.getWithSectionsWithLibraryItems(id)

    override fun sessionsInTimeframe (timeframe: Timeframe) : Flow<List<SessionWithSectionsWithLibraryItems>> {
        assert (timeframe.first < timeframe.second)
        return sessionDao.getFromTimeframe(
            startTimestamp = timeframe.first,
            endTimestamp = timeframe.second
        )
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
        sectionsCreationAttributes: List<SectionCreationAttributes>
    ): Pair<UUID, List<UUID>> {
        return sessionDao.insert(
            sessionCreationAttributes,
            sectionsCreationAttributes
        )
    }

    /** Delete / Restore */
    override suspend fun delete(sessions: List<Session>) {
        sessionDao.delete(sessions.map { it.id })
    }

    override suspend fun restore(sessions: List<Session>) {
        sessionDao.restore(sessions.map { it.id })
    }

    /** Clean */
    override suspend fun clean() {
        sessionDao.clean()
    }
}