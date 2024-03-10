package app.musikus.repository

import app.musikus.usecase.activesession.ActiveSessionRepository
import app.musikus.usecase.activesession.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class ActiveSessionRepositoryImpl : ActiveSessionRepository {

    private val sessionState = MutableStateFlow<SessionState?>(null)

    override suspend fun setSessionState(sessionState: SessionState) {
        this.sessionState.update { sessionState }
    }

    override fun getSessionState() = sessionState.asStateFlow()
}