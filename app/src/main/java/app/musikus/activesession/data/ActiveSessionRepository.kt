/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.activesession.data

import app.musikus.activesession.domain.SessionState
import kotlinx.coroutines.flow.Flow


interface ActiveSessionRepository {
    suspend fun setSessionState(
        sessionState: SessionState
    )
    fun getSessionState() : Flow<SessionState?>

    fun reset()

    fun isRunning(): Boolean
}