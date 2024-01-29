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
import app.musikus.shared.TopBarUiState
import app.musikus.usecase.sessions.SessionsUseCases
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
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
    val monthData: List<MonthUiDatum>,
    val selectedSessions: Set<UUID>,

    val showHint: Boolean,
)

data class SessionsUiState(
    val topBarUiState: SessionsTopBarUiState,
    val actionModeUiState: SessionsActionModeUiState,
    val contentUiState: SessionsContentUiState,
)

data class MonthUiDatum(
    val month: String,
    val specificMonth: Int,
    val dayData: List<DayUiDatum>,
)
data class DayUiDatum(
    val date: String,
    val totalPracticeDuration: String,
    val sessions: List<SessionWithSectionsWithLibraryItems>,
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionsUseCases: SessionsUseCases,
) : ViewModel() {

    private var _sessionIdsCache = emptyList<UUID>()


    /** Imported Flows */
    private val sessionsForDaysForMonths = sessionsUseCases.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Own state flow */

    private val _selectedSessions = MutableStateFlow<Set<UUID>>(emptySet())

    private val _expandedMonths = MutableStateFlow<Set<Int>>(emptySet())


    /** Combining imported and own state flows */

    private var areSessionsLoaded = false

    private val monthUiData = combine(
        sessionsForDaysForMonths,
        _expandedMonths,
    ) { sessionsForDaysForMonths, expandedMonths ->
        if (sessionsForDaysForMonths.isEmpty()) return@combine emptyList()

        // if there are no expanded months, expand the latest month
        if (!areSessionsLoaded) {
            areSessionsLoaded = true
            _expandedMonths.update { setOf(sessionsForDaysForMonths.first().specificMonth) }
            return@combine emptyList()
        }

        sessionsForDaysForMonths.map { sessionsForDaysForMonth ->
            MonthUiDatum(
                month = sessionsForDaysForMonth.month.name,
                specificMonth = sessionsForDaysForMonth.specificMonth,
                dayData =
                    if(sessionsForDaysForMonth.specificMonth in expandedMonths)
                        sessionsForDaysForMonth.sessionsForDays.map { sessionsForDay ->
                            DayUiDatum(
                                date = sessionsForDay.day,
                                totalPracticeDuration = getDurationString(
                                    sessionsForDay.totalPracticeDuration,
                                    DurationFormat.HUMAN_PRETTY
                                ).toString(),
                                sessions = sessionsForDay.sessions,
                            )
                        }
                    else emptyList()
            )
        }
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
        monthUiData,
        _selectedSessions,
    ) { monthUiData, selectedSessions ->
        SessionsContentUiState(
            monthData = monthUiData,
            selectedSessions = selectedSessions,
            showHint = monthUiData.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionsContentUiState(
            monthData = monthUiData.value,
            selectedSessions = _selectedSessions.value,
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
        editSession(_selectedSessions.value.single())
        clearActionMode()
    }

    fun onDeleteAction() {
        viewModelScope.launch {
            _sessionIdsCache = _selectedSessions.value.toList()
            sessionsUseCases.delete(_selectedSessions.value.toList())
            clearActionMode()
        }
    }

    fun onRestoreAction() {
        viewModelScope.launch {
            sessionsUseCases.restore(_sessionIdsCache.toList())
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
        sessionId: UUID,
        longClick: Boolean = false
    ) {
        if(longClick) {
            _selectedSessions.update { it + sessionId }
            return
        }

        // Short click
        if(!actionModeUiState.value.isActionMode) {
            // go to session detail screen
        } else {
            if(_selectedSessions.value.contains(sessionId)) {
                _selectedSessions.update { it - sessionId }
            } else {
                _selectedSessions.update { it + sessionId }
            }
        }
    }
}