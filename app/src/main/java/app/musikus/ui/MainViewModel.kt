/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.datastore.ColorSchemeSelections
import app.musikus.datastore.ThemeSelections
import app.musikus.usecase.activesession.ActiveSessionUseCases
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
                showSnackbar(event.message, event.onUndo)
            }
        }
    }

    /**
     * Private state mutators
     */

    private fun showSnackbar(message: String, onUndo: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = _snackbarHost.value.showSnackbar(
                message,
                actionLabel = if (onUndo != null) "Undo" else null,
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    onUndo?.invoke()
                }

                SnackbarResult.Dismissed -> {
                    // do nothing
                }
            }
        }
    }
}