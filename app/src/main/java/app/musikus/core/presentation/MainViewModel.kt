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
import app.musikus.core.presentation.components.MultiFabState
import app.musikus.core.presentation.components.showSnackbar
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.settings.domain.ThemeSelections
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias MainUiEventHandler = (MainUiEvent) -> Unit

sealed class MainUiEvent {
    data class ShowSnackbar(val message: String, val onUndo: (() -> Unit)? = null) : MainUiEvent()
    data object ExpandMultiFab : MainUiEvent()
    data object CollapseMultiFab : MainUiEvent()
    data object OpenMainMenu : MainUiEvent()
}

sealed class MainEvent {
    data object OpenMainDrawer : MainEvent()
}

data class MainUiState(
    val activeTheme: ThemeSelections?,
    val activeColorScheme: ColorSchemeSelections?,
    val snackbarHost: SnackbarHostState,
    val isSessionRunning: Boolean,
    val multiFabState: MultiFabState,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    userPreferencesUseCases: UserPreferencesUseCases,
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

    // Theme
    private val _activeTheme = userPreferencesUseCases.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Color Scheme
    private val _activeColorScheme = userPreferencesUseCases.getColorScheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Content Scrim over NavBar for Multi FAB etc.
    private val _multiFabState = MutableStateFlow(MultiFabState.COLLAPSED)

    /**
     * Imported flows
     */
    private val runningItem = activeSessionUseCases.getRunningItem().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Composing the ui state
     */

    val uiState = combine(
        _activeTheme,
        _activeColorScheme,
        _snackbarHost,
        runningItem,
        _multiFabState
    ) { activeTheme, activeColorScheme, snackbarHost, runningItem, multiFabState ->
        MainUiState(
            activeTheme = activeTheme,
            activeColorScheme = activeColorScheme,
            snackbarHost = snackbarHost,
            isSessionRunning = runningItem != null,
            multiFabState = multiFabState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            activeTheme = _activeTheme.value,
            activeColorScheme = _activeColorScheme.value,
            snackbarHost = _snackbarHost.value,
            isSessionRunning = runningItem.value != null,
            multiFabState = _multiFabState.value
        )
    )

    fun onUiEvent(event: MainUiEvent) {
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
        }
    }
}
