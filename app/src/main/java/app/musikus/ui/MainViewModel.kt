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
import app.musikus.ui.components.MultiFabState
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


typealias MainUiEventHandler = (MainUiEvent) -> Unit

sealed class MainUiEvent {
    data object ShowMainMenu: MainUiEvent()
    data object HideMainMenu: MainUiEvent()
    data object ShowThemeSubMenu: MainUiEvent()
    data object HideThemeSubMenu: MainUiEvent()
    data object ShowExportImportDialog: MainUiEvent()
    data object HideExportImportDialog: MainUiEvent()
    data class SetTheme(val theme: ThemeSelections): MainUiEvent()
    data object CollapseMultiFab: MainUiEvent()
    data class ChangeMultiFabState(val state: MultiFabState): MainUiEvent()
    data class ShowSnackbar(val message: String, val onUndo: (() -> Unit)? = null): MainUiEvent()


}

data class MainMenuUiState(
    val show: Boolean,
    val showThemeSubMenu: Boolean,
)
data class MainUiState(
    val menuUiState: MainMenuUiState,
    val showExportImportDialog: Boolean,
    val multiFabState: MultiFabState,
    val activeTheme: ThemeSelections?,
    var snackbarHost: SnackbarHostState,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesUseCases: UserPreferencesUseCases,
) : ViewModel() {

    /** Menu */
    private val _showMainMenu = MutableStateFlow(false)
    private val _showThemeSubMenu = MutableStateFlow(false)

    /** Import/Export */
    private val _showExportImportDialog = MutableStateFlow(false)

    /** Content Scrim over NavBar for Multi FAB etc */
    private val _multiFabState = MutableStateFlow(MultiFabState.COLLAPSED)

    /** Snackbar */
    private val _snackbarHost = MutableStateFlow(SnackbarHostState())

    /** Theme */
    private val _activeTheme = userPreferencesUseCases.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun onUiEvent(event: MainUiEvent) {
        when(event) {
            is MainUiEvent.ShowMainMenu -> {
                onShowMainMenuChanged(true)
            }
            is MainUiEvent.HideMainMenu -> {
                onShowMainMenuChanged(false)
            }
            is MainUiEvent.ShowThemeSubMenu -> {
                onShowThemeSubMenuChanged(true)
            }
            is MainUiEvent.HideThemeSubMenu -> {
                onShowThemeSubMenuChanged(false)
            }
            is MainUiEvent.ShowExportImportDialog -> {
                onShowExportImportDialogChanged(true)
            }
            is MainUiEvent.HideExportImportDialog -> {
                onShowExportImportDialogChanged(false)
            }
            is MainUiEvent.SetTheme -> {
                setTheme(event.theme)
            }
            is MainUiEvent.ChangeMultiFabState -> {
                onMultiFabStateChanged(event.state)
            }
            is MainUiEvent.CollapseMultiFab -> {
                onMultiFabStateChanged(MultiFabState.COLLAPSED)
            }
            is MainUiEvent.ShowSnackbar -> {
                showSnackbar(event.message, event.onUndo)
            }
        }
    }

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

    /** UI State */
    private val menuUiState = combine(
        _showMainMenu,
        _showThemeSubMenu
    ) { showMainMenu, showThemeSubMenu ->
        MainMenuUiState(
            show = showMainMenu,
            showThemeSubMenu = showThemeSubMenu,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainMenuUiState(
            show = _showMainMenu.value,
            showThemeSubMenu = _showThemeSubMenu.value,
        )
    )

    val uiState = combine(
        menuUiState,
        _showExportImportDialog,
        _multiFabState,
        _activeTheme,
        _snackbarHost,
    ) { menuUiState, showExportImportDialog, multiFabState, activeTheme, snackbarHost ->
        MainUiState(
            menuUiState = menuUiState,
            showExportImportDialog = showExportImportDialog,
            multiFabState = multiFabState,
            activeTheme = activeTheme,
            snackbarHost = snackbarHost
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            menuUiState = menuUiState.value,
            showExportImportDialog = _showExportImportDialog.value,
            multiFabState = _multiFabState.value,
            activeTheme = _activeTheme.value,
            snackbarHost = _snackbarHost.value
        )
    )

    /** State modifiers */
    private fun onShowMainMenuChanged(show: Boolean) {
        _showMainMenu.update { show }
    }

    private fun onShowThemeSubMenuChanged(show: Boolean) {
        _showThemeSubMenu.update { show }
    }

    private fun onMultiFabStateChanged(state: MultiFabState) {
        _multiFabState.update { state }
    }

    private fun onShowExportImportDialogChanged(show: Boolean) {
        _showExportImportDialog.update { show }
    }
}