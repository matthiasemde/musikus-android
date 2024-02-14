/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.datastore.ThemeSelections
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


typealias MainUiEventHandler = (MainUiEvent) -> Unit

sealed class MainUiEvent {
    data class SetTheme(val theme: ThemeSelections): MainUiEvent()
    data class ShowSnackbar(val message: String, val onUndo: (() -> Unit)? = null): MainUiEvent()
}

data class MainUiState(
    val activeTheme: ThemeSelections?,
    var snackbarHost: SnackbarHostState,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesUseCases: UserPreferencesUseCases,
) : ViewModel() {

    /** Snackbar */
    private val _snackbarHost = MutableStateFlow(SnackbarHostState())

    /** Theme */
    private val _activeTheme = userPreferencesUseCases.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    /**
     * Composing the ui state
     */

    val uiState = combine(
        _activeTheme,
        _snackbarHost,
    ) { activeTheme, snackbarHost ->
        MainUiState(
            activeTheme = activeTheme,
            snackbarHost = snackbarHost
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            activeTheme = _activeTheme.value,
            snackbarHost = _snackbarHost.value
        )
    )


    fun onUiEvent(event: MainUiEvent) {
        when(event) {
            is MainUiEvent.SetTheme -> {
                setTheme(event.theme)
            }
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
                actionLabel = if(onUndo != null) "Undo" else null,
                duration = SnackbarDuration.Long
            )
            when(result) {
                SnackbarResult.ActionPerformed -> {
                    onUndo?.invoke()
                }
                SnackbarResult.Dismissed -> {
                    // do nothing
                }
            }
        }
    }

    private fun setTheme(theme: ThemeSelections) {
        viewModelScope.launch {
            userPreferencesUseCases.selectTheme(theme)
        }
    }
}