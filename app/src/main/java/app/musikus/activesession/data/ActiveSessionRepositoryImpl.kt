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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class ActiveSessionRepositoryImpl : ActiveSessionRepository {

    private val sessionState = MutableStateFlow<SessionState?>(null)

    override suspend fun setSessionState(sessionState: SessionState) {
        this.sessionState.update { sessionState }
    }
    override fun getSessionState() = sessionState.asStateFlow()

    override fun reset() {
        sessionState.update { null }
    }

    override fun isRunning(): Boolean {
        return sessionState.value != null
    }
}