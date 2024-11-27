/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.activesession.domain.usecase.ActiveSessionUseCases
import app.musikus.core.domain.usecase.CoreUseCases
import app.musikus.core.presentation.components.MultiFabState
import app.musikus.core.presentation.components.showSnackbar
import app.musikus.menu.domain.ColorSchemeSelections
import app.musikus.menu.domain.ThemeSelections
import app.musikus.menu.domain.usecase.SettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias MainUiEventHandler = (MainUiEvent) -> Boolean

sealed class MainUiEvent {
    data class ShowSnackbar(val message: String, val onUndo: (() -> Unit)? = null) : MainUiEvent()
    data object ExpandMultiFab : MainUiEvent()
    data object CollapseMultiFab : MainUiEvent()
    data object OpenMainMenu : MainUiEvent()
    data object DismissAnnouncement : MainUiEvent()
}

sealed class MainEvent {
    data object OpenMainDrawer : MainEvent()
}

data class ThemeUiState(
    val activeTheme: ThemeSelections?,
    val activeColorScheme: ColorSchemeSelections?
)

data class MainUiState(
    val themeUiState: ThemeUiState,
    val snackbarHost: SnackbarHostState,
    val isSessionRunning: Boolean,
    val multiFabState: MultiFabState,
    val showAnnouncement: Boolean
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val coreUseCases: CoreUseCases,
    settingsUseCases: SettingsUseCases,
    activeSessionUseCases: ActiveSessionUseCases
) : AndroidViewModel(application) {

    /**
     * Private state variables
     */

    // Snackbar
    private val _snackbarHost = MutableStateFlow(SnackbarHostState())

    // Main Menu Drawer
    private val _eventChannel = Channel<MainEvent>()
    val eventChannel = _eventChannel.receiveAsFlow()

    // Content Scrim over NavBar for Multi FAB etc.
    private val _multiFabState = MutableStateFlow(MultiFabState.COLLAPSED)

    /**
     * Imported flows
     */
    private val showAnnouncement = coreUseCases.getIdOfLastSeenAnnouncementSeen().map {
        it < CURRENT_ANNOUNCEMENT_ID
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val runningItem = activeSessionUseCases.getRunningItem().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val activeTheme = settingsUseCases.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val activeColorScheme = settingsUseCases.getColorScheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Composing the ui state
     */

    val themeUiState = combine(
        activeTheme,
        activeColorScheme
    ) { activeTheme, activeColorScheme ->
        ThemeUiState(
            activeTheme = activeTheme,
            activeColorScheme = activeColorScheme
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeUiState(
            activeTheme = activeTheme.value,
            activeColorScheme = activeColorScheme.value
        )
    )

    val uiState = combine(
        themeUiState,
        _snackbarHost,
        runningItem,
        _multiFabState,
        showAnnouncement
    ) { themeUiState, snackbarHost, runningItem, multiFabState, showAnnouncement ->
        MainUiState(
            themeUiState = themeUiState,
            snackbarHost = snackbarHost,
            isSessionRunning = runningItem != null,
            multiFabState = multiFabState,
            showAnnouncement = showAnnouncement
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            themeUiState = themeUiState.value,
            snackbarHost = _snackbarHost.value,
            isSessionRunning = runningItem.value != null,
            multiFabState = _multiFabState.value,
            showAnnouncement = showAnnouncement.value
        )
    )

    fun onUiEvent(event: MainUiEvent): Boolean {
        when (event) {
            is MainUiEvent.ShowSnackbar -> {
                showSnackbar(
                    context = application,
                    scope = viewModelScope,
                    hostState = _snackbarHost.value,
                    message = event.message,
                    onUndo = event.onUndo
                )
            }
            is MainUiEvent.ExpandMultiFab -> {
                _multiFabState.update { MultiFabState.EXPANDED }
            }
            is MainUiEvent.CollapseMultiFab -> {
                _multiFabState.update { MultiFabState.COLLAPSED }
            }
            is MainUiEvent.OpenMainMenu -> {
                viewModelScope.launch {
                    _eventChannel.send(MainEvent.OpenMainDrawer)
                }
            }
            is MainUiEvent.DismissAnnouncement -> {
                viewModelScope.launch {
                    coreUseCases.confirmAnnouncementMessage()
                }
            }
        }

        // events are consumed by default
        return true
    }
}
