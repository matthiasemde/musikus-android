package app.musikus.usecase.activesession

import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

class GetStartTimeUseCase (
    private val activeSessionRepository: ActiveSessionRepository
){
    suspend operator fun invoke() : ZonedDateTime {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("State is null. Cannot get start time!")

        return state.startTimestamp
    }
}