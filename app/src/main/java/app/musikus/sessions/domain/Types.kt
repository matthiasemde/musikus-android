/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.domain

import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.library.data.daos.LibraryItem
import app.musikus.sessions.data.daos.Section
import app.musikus.sessions.data.daos.Session
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SectionUpdateAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import app.musikus.sessions.data.entities.SessionUpdateAttributes
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.Timeframe
import app.musikus.core.domain.musikusFormat
import kotlinx.coroutines.flow.Flow
import java.time.Month
import java.util.UUID
import kotlin.time.Duration

interface SessionRepository {
    val sessions : Flow<List<Session>>
    val sections : Flow<List<Section>>

    val orderedSessionsWithSectionsWithLibraryItems : Flow<List<SessionWithSectionsWithLibraryItems>>
    suspend fun getSession(id: UUID) : SessionWithSectionsWithLibraryItems

    fun sessionsInTimeframe (timeframe: Timeframe) : Flow<List<SessionWithSectionsWithLibraryItems>>

    suspend fun sectionsForSession(sessionId: UUID): List<Section>

    suspend fun sectionsForGoal (goal: GoalInstanceWithDescriptionWithLibraryItems) : Flow<List<Section>>
    suspend fun sectionsForGoal (
        instance: GoalInstance,
        description: GoalDescription,
        libraryItems: List<LibraryItem>
    ) : Flow<List<Section>>

    fun getLastSectionsForItems(items: List<LibraryItem>) : Flow<List<Section>>

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

data class SessionsForDaysForMonth (
    val specificMonth: Int,
    val sessionsForDays: List<SessionsForDay>
) {
    val month: Month by lazy {
        this.sessionsForDays.first().sessions.first().startTimestamp.month
    }
}

data class SessionsForDay (
    val specificDay: Int,
    val totalPracticeDuration: Duration,
    val sessions: List<SessionWithSectionsWithLibraryItems>
) {
    val day: String by lazy {
        this.sessions.first().startTimestamp.musikusFormat(DateFormat.DAY_AND_MONTH)
    }
}