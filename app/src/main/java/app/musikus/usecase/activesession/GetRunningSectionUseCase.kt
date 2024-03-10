package app.musikus.usecase.activesession


import app.musikus.database.daos.LibraryItem
import app.musikus.utils.TimeProvider
import app.musikus.utils.minus
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import kotlin.time.Duration

class GetRunningSectionUseCase (
    private val activeSessionRepository: ActiveSessionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        at: ZonedDateTime = timeProvider.now()
    ) : Pair<LibraryItem, Duration> {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("State is null. Cannot get running section!")

        val duration = if (state.isPaused) {
            if (state.currentPauseStartTimestamp == null) {
                throw IllegalStateException("CurrentPauseTimestamp is null although isPaused is true.")
            }
            state.currentPauseStartTimestamp - state.startTimestampSectionPauseCompensated
        } else {
            at - state.startTimestampSectionPauseCompensated
        }
        return Pair(state.currentSectionItem, duration)
    }
}