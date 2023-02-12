/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.SessionWithSectionsWithLibraryItems
import de.practicetime.practicetime.repository.SessionRepository
import de.practicetime.practicetime.shared.TopBarUiState
import de.practicetime.practicetime.utils.getSpecificDay
import de.practicetime.practicetime.utils.getSpecificMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SessionsForDaysForMonth (
    val specificMonth: Int,
    val sessionsForDays: List<SessionsForDay>
)

data class SessionsForDay (
    val specificDay: Int,
    val totalPracticeDuration: Int,
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

class SessionsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Database */
    private val database = PTDatabase.getInstance(application)

    /** Repositories */
    private val sessionRepository = SessionRepository(database)

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
        var (currentDay, currentMonth) = sessions.first()
            .sections.first()
            .section.timestamp.let { timestamp ->
                Pair(getSpecificDay(timestamp), getSpecificMonth(timestamp))
            }

        var firstSessionOfDayIndex = 0
        var totalPracticeDuration = 0

        val sessionsForDaysForMonth = mutableListOf<SessionsForDay>()

        // then loop trough all of the sessions...
        sessions.forEachIndexed { index, session ->
            // ...get the month and day...
            val sessionTimestamp = session.sections.first().section.timestamp
            val (day, month) = sessionTimestamp.let { timestamp ->
                Pair(getSpecificDay(timestamp), getSpecificMonth(timestamp))
            }

            totalPracticeDuration += session.sections.sumOf { it.section.duration ?: 0 }

            // ...and compare them to the current day first.
            // if it differs, create a new SessionsForDay object
            // with the respective subList of sessions
            if(day == currentDay) return@forEachIndexed

            sessionsForDaysForMonth.add(SessionsForDay(
                specificDay = currentDay,
                totalPracticeDuration = totalPracticeDuration,
                sessions = sessions.slice(firstSessionOfDayIndex until index)
            ))

            // reset / set tracking variables appropriately
            currentDay = day
            firstSessionOfDayIndex = index
            totalPracticeDuration = 0

            // then compare the month to the current month.
            // if it differs, create a new SessionsForDaysForMonth object
            // storing the specific month along with the list of SessionsForDay objects
            if(month == currentMonth) return@forEachIndexed

            sessionsForDaysForMonths.add(SessionsForDaysForMonth(
                specificMonth = currentMonth,
                sessionsForDays = sessionsForDaysForMonth.toList()
            ))

            // set tracking variable and reset list
            currentMonth = month
            sessionsForDaysForMonth.clear()
        }

        // importantly, add the last SessionsForDaysForMonth object
        sessionsForDaysForMonth.add(SessionsForDay(
            specificDay = currentDay,
            totalPracticeDuration = totalPracticeDuration,
            sessions = sessions.slice(firstSessionOfDayIndex until sessions.size)
        ))
        sessionsForDaysForMonths.add(SessionsForDaysForMonth(
            specificMonth = currentMonth,
            sessionsForDays = sessionsForDaysForMonth
        ))

        // if there are no expanded months, expand the latest month
        if(_expandedMonths.value.isEmpty()) {
            _expandedMonths.update { setOf(sessionsForDaysForMonths.last().specificMonth) }
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

    val sessionsUiState = combine(
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

    fun onEditAction() {
        _selectedSessions.value.firstOrNull()?.let {
            // TODO go to edit session screen with sessionToEdit
        } ?: Log.d("SessionsViewModel", "Tried to edit with no session selected")
        clearActionMode()
    }

    fun onDeleteAction() {
        viewModelScope.launch {
            sessionRepository.delete(_selectedSessions.value.map { it.session })
            clearActionMode()
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
            // TODO go to session detail screen
        } else {
            if(_selectedSessions.value.contains(session)) {
                _selectedSessions.update { it - session }
            } else {
                _selectedSessions.update { it + session }
            }
        }
    }
}