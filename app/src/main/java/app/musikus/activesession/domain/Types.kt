/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.domain

import app.musikus.library.data.daos.LibraryItem
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration

interface ActiveSessionRepository {
    suspend fun setSessionState(
        sessionState: SessionState
    )
    fun getSessionState(): Flow<SessionState?>

    fun reset()

    fun isRunning(): Boolean
}

data class PracticeSection(
    val id: UUID,
    val libraryItem: LibraryItem,
    val pauseDuration: Duration, // set when section is completed
    val duration: Duration, // set when section is completed
    val startTimestamp: ZonedDateTime
)

data class SessionState(
    val completedSections: List<PracticeSection>,
    val currentSectionItem: LibraryItem,
    val startTimestamp: ZonedDateTime,
    val startTimestampSection: ZonedDateTime,
    val startTimestampSectionPauseCompensated: ZonedDateTime,
    val currentPauseStartTimestamp: ZonedDateTime?,
    val isPaused: Boolean,
)
