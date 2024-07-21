/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.activesession.domain.usecase.ActiveSessionUseCases
import app.musikus.core.presentation.components.showSnackbar
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.settings.domain.ThemeSelections
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


typealias MainUiEventHandler = (MainUiEvent) -> Unit

sealed class MainUiEvent {
    data class ShowSnackbar(val message: String, val onUndo: (() -> Unit)? = null): MainUiEvent()
}

data class MainUiState(
    val activeTheme: ThemeSelections?,
    val activeColorScheme: ColorSchemeSelections?,
    var snackbarHost: SnackbarHostState,
    var isSessionRunning: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesUseCases: UserPreferencesUseCases,
    activeSessionUseCases: ActiveSessionUseCases
) : ViewModel() {


    /**
     * Private state variables
     */

    /** Snackbar */
    private val _snackbarHost = MutableStateFlow(SnackbarHostState())

    /** Theme */
    private val _activeTheme = userPreferencesUseCases.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _activeColorScheme = userPreferencesUseCases.getColorScheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

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
        runningItem
    ) { activeTheme, activeColorScheme, snackbarHost, runningItem ->
        MainUiState(
            activeTheme = activeTheme,
            activeColorScheme = activeColorScheme,
            snackbarHost = snackbarHost,
            isSessionRunning = runningItem != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            activeTheme = _activeTheme.value,
            activeColorScheme = _activeColorScheme.value,
            snackbarHost = _snackbarHost.value,
            isSessionRunning = runningItem.value != null
        )
    )


    fun onUiEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.ShowSnackbar -> {
                showSnackbar(
                    viewModelScope,
                    _snackbarHost.value,
                    event.message,
                    event.onUndo
                )
            }
        }
    }
}