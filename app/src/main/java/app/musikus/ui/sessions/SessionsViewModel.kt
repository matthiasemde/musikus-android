/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.daos.Session
import app.musikus.repository.SessionRepository
import app.musikus.shared.TopBarUiState
import app.musikus.utils.specificDay
import app.musikus.utils.specificMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class SessionsForDaysForMonth (
    val specificMonth: Int,
    val sessionsForDays: List<SessionsForDay>
)

data class SessionsForDay (
    val specificDay: Int,
    val totalPracticeDuration: Duration,
    val sessions: List<SessionWithSectionsWithLibraryItems>
)

/** Ui state data classes */

data class SessionsTopBarUiState(
    override val title: String,
    override val showBackButton: Boolean,
) : TopBarUiState

data class SessionsActionModeUiState(
    val isActionMode: Boolean,
    val numberOfSelections: Int,
)

data class SessionsContentUiState (
    val sessionsForDaysForMonths: List<SessionsForDaysForMonth>,
    val selectedSessions: Set<SessionWithSectionsWithLibraryItems>,
    val expandedMonths: Set<Int>,

    val showHint: Boolean,
)

data class SessionsUiState(
    val topBarUiState: SessionsTopBarUiState,
    val actionModeUiState: SessionsActionModeUiState,
    val contentUiState: SessionsContentUiState,
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository : SessionRepository,
) : ViewModel() {

    private var _sessionsCache = emptyList<Session>()


    /** Imported Flows */
    private val sessions = sessionRepository.sessionsWithSectionsWithLibraryItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Own state flow */

    private val _selectedSessions = MutableStateFlow<Set<SessionWithSectionsWithLibraryItems>>(emptySet())

    private val _expandedMonths = MutableStateFlow<Set<Int>>(emptySet())


    /** Combining imported and own state flows */


    private val sessionsForDaysForMonths = sessions.map { sessions ->
        if(sessions.isEmpty()) {
            return@map emptyList()
        }

        val sessionsForDaysForMonths = mutableListOf<SessionsForDaysForMonth>()

        // initialize variables to keep track of the current month, current day,
        // the index of its first session and the total duration of the current day
        var (currentDay, currentMonth) = sessions.first().startTimestamp.let { timestamp ->
            Pair(timestamp.specificDay, timestamp.specificMonth)
        }

        var firstSessionOfDayIndex = 0
        var totalPracticeDuration = 0.seconds

        val sessionsForDaysForMonth = mutableListOf<SessionsForDay>()

        // then loop trough all of the sessions...
        sessions.forEachIndexed { index, session ->
            // ...get the month and day...
            val sessionTimestamp = session.startTimestamp
            val (day, month) = sessionTimestamp.let { timestamp ->
                Pair(timestamp.specificDay, timestamp.specificMonth)
            }

            val sessionPracticeDuration = session.sections.sumOf {
                it.section.duration.inWholeSeconds
            }.seconds

            // ...and compare them to the current day first.
            if(day == currentDay) {
                totalPracticeDuration += sessionPracticeDuration
                return@forEachIndexed
            }

            // if it differs, create a new SessionsForDay object
            // with the respective subList of sessions
            sessionsForDaysForMonth.add(
                SessionsForDay(
                    specificDay = currentDay,
                    totalPracticeDuration = totalPracticeDuration,
                    sessions = sessions.slice(firstSessionOfDayIndex until index)
                )
            )

            // reset / set tracking variables appropriately
            currentDay = day
            firstSessionOfDayIndex = index
            totalPracticeDuration = sessionPracticeDuration

            // then compare the month to the current month.
            // if it differs, create a new SessionsForDaysForMonth object
            // storing the specific month along with the list of SessionsForDay objects
            if(month == currentMonth) return@forEachIndexed

            sessionsForDaysForMonths.add(
                SessionsForDaysForMonth(
                    specificMonth = currentMonth,
                    sessionsForDays = sessionsForDaysForMonth.toList()
                )
            )

            // set tracking variable and reset list
            currentMonth = month
            sessionsForDaysForMonth.clear()
        }

        // importantly, add the last SessionsForDaysForMonth object
        sessionsForDaysForMonth.add(
            SessionsForDay(
                specificDay = currentDay,
                totalPracticeDuration = totalPracticeDuration,
                sessions = sessions.slice(firstSessionOfDayIndex until sessions.size)
            )
        )
        sessionsForDaysForMonths.add(
            SessionsForDaysForMonth(
                specificMonth = currentMonth,
                sessionsForDays = sessionsForDaysForMonth
            )
        )

        // if there are no expanded months, expand the latest month
        if(_expandedMonths.value.isEmpty()) {
            _expandedMonths.update { setOf(sessionsForDaysForMonths.first().specificMonth) }
        }

        // finally return the list as immutable
        sessionsForDaysForMonths.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Composing the Ui state */
    private val topBarUiState = MutableStateFlow(
        SessionsTopBarUiState(
            title = "Sessions",
            showBackButton = false,
        )
    )

    private val actionModeUiState = _selectedSessions.map { selectedSessions ->
        SessionsActionModeUiState(
            isActionMode = selectedSessions.isNotEmpty(),
            numberOfSelections = selectedSessions.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionsActionModeUiState(
            isActionMode = false,
            numberOfSelections = 0,
        )
    )

    private val contentUiState = combine(
        sessionsForDaysForMonths,
        _selectedSessions,
        _expandedMonths,
    ) { sessionsForDaysForMonths, selectedSessions, expandedMonths ->
        SessionsContentUiState(
            sessionsForDaysForMonths = sessionsForDaysForMonths,
            selectedSessions = selectedSessions,
            expandedMonths = expandedMonths,
            showHint = sessionsForDaysForMonths.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionsContentUiState(
            sessionsForDaysForMonths = emptyList(),
            selectedSessions = emptySet(),
            expandedMonths = emptySet(),
            showHint = true,
        )
    )

    val uiState = combine(
        topBarUiState,
        actionModeUiState,
        contentUiState,
    ) { topBarUiState, actionModeUiState, contentUiState ->
        SessionsUiState(
            topBarUiState = topBarUiState,
            actionModeUiState = actionModeUiState,
            contentUiState = contentUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionsUiState(
            topBarUiState = topBarUiState.value,
            actionModeUiState = actionModeUiState.value,
            contentUiState = contentUiState.value,
        )
    )

    /** Mutators */

    fun onEditAction(editSession: (id: UUID) -> Unit) {
        assert(_selectedSessions.value.size == 1)
        editSession(_selectedSessions.value.first().session.id)
        clearActionMode()
    }

    fun onDeleteAction() {
        viewModelScope.launch {
            _sessionsCache = _selectedSessions.value.map { it.session }
            sessionRepository.delete(_sessionsCache)
            clearActionMode()
        }
    }

    fun onRestoreAction() {
        viewModelScope.launch {
            sessionRepository.restore(_sessionsCache)
        }
    }

    fun clearActionMode() {
        _selectedSessions.update{ emptySet() }
    }

    fun onMonthHeaderClicked(specificMonth: Int) {
        _expandedMonths.update {
            if(it.contains(specificMonth)) {
                it - specificMonth
            } else {
                it + specificMonth
            }
        }
    }

    fun onSessionClicked(
        session: SessionWithSectionsWithLibraryItems,
        longClick: Boolean = false
    ) {
        if(longClick) {
            _selectedSessions.update { it + session }
            return
        }

        // Short click
        if(!actionModeUiState.value.isActionMode) {
            // go to session detail screen
        } else {
            if(_selectedSessions.value.contains(session)) {
                _selectedSessions.update { it - session }
            } else {
                _selectedSessions.update { it + session }
            }
        }
    }
}