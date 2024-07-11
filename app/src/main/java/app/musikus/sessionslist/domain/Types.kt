/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessionslist.domain

import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.library.data.daos.LibraryItem
import app.musikus.sessionslist.data.daos.Section
import app.musikus.sessionslist.data.daos.Session
import app.musikus.sessionslist.data.entities.SectionCreationAttributes
import app.musikus.sessionslist.data.entities.SectionUpdateAttributes
import app.musikus.sessionslist.data.entities.SessionCreationAttributes
import app.musikus.sessionslist.data.entities.SessionUpdateAttributes
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.Timeframe
import app.musikus.core.domain.musikusFormat
import kotlinx.coroutines.flow.Flow
import java.time.Month
import java.util.UUID
import kotlin.time.Duration

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