package app.musikus.usecase.sessions

import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.utils.DateFormat
import app.musikus.utils.musikusFormat
import java.time.Month
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