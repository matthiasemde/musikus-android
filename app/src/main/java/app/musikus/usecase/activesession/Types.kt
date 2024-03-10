package app.musikus.usecase.activesession

import app.musikus.database.daos.LibraryItem
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration


interface ActiveSessionRepository {
    suspend fun setSessionState(
        sessionState: SessionState
    )

    fun getSessionState() : Flow<SessionState?>
}

data class PracticeSection(
    val id: UUID,
    val libraryItem: LibraryItem,
    val pauseDuration: Duration,   // set when section is completed
    val duration: Duration         // set when section is completed
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