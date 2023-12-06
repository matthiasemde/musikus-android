/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.ui

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.MusikusDatabase
import app.musikus.datastore.ThemeSelections
import app.musikus.repository.GoalRepository
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.shared.MultiFabState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class MainMenuUiState(
    val show: Boolean,
    val showThemeSubMenu: Boolean,
)
data class MainUiState(
    val menuUiState: MainMenuUiState,
    val showExportImportDialog: Boolean,
    val multiFabState: MultiFabState,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository : UserPreferencesRepository,
    libraryRepository : LibraryRepository,
    goalRepository: GoalRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {

    /** Initialization */
    init {
        // Clean up soft-deleted items
        viewModelScope.launch {
            Log.d("Musikus", "Clean up soft-deleted items")
            sessionRepository.clean()
            goalRepository.clean()
            libraryRepository.clean()
        }
    }

    /** Menu */
    private val _showMainMenu = MutableStateFlow(false)
    private val _showThemeSubMenu = MutableStateFlow(false)

    /** Import/Export */
    private val _showExportImportDialog = MutableStateFlow(false)

    /** Content Scrim over NavBar for Multi FAB etc */
    private val _multiFabState = MutableStateFlow(MultiFabState.COLLAPSED)

    /** Snackbar */
    val snackbarHostState = MutableStateFlow(SnackbarHostState())

    fun showSnackbar(message: String, onUndo: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = snackbarHostState.value.showSnackbar(
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

    /** Theme */

    val activeTheme = userPreferencesRepository.userPreferences.map { it.theme }

    fun setTheme(theme: ThemeSelections) {
        viewModelScope.launch {
            userPreferencesRepository.updateTheme(theme)
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
        _multiFabState
    ) { menuUiState, showExportImportDialog, multiFabState ->
        MainUiState(
            menuUiState = menuUiState,
            showExportImportDialog = showExportImportDialog,
            multiFabState = multiFabState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            menuUiState = menuUiState.value,
            showExportImportDialog = _showExportImportDialog.value,
            multiFabState = _multiFabState.value,
        )
    )

    /** State modifiers */
    private fun onShowMainMenuChanged(show: Boolean) {
        _showMainMenu.update { show }
    }

    fun showMainMenu() {
        onShowMainMenuChanged(true)
    }

    fun hideMainMenu() {
        onShowMainMenuChanged(false)
    }

    private fun onShowThemeSubMenuChanged(show: Boolean) {
        _showThemeSubMenu.update { show }
    }

    fun showThemeSubMenu() {
        onShowThemeSubMenuChanged(true)
    }

    fun hideThemeSubMenu() {
        onShowThemeSubMenuChanged(false)
    }

    fun onMultiFabStateChanged(state: MultiFabState) {
        _multiFabState.update { state }
    }

    fun collapseMultiFab() {
        onMultiFabStateChanged(MultiFabState.COLLAPSED)
    }
    private fun onShowExportImportDialogChanged(show: Boolean) {
        _showExportImportDialog.update { show }
    }

    fun showExportImportDialog() {
        onShowExportImportDialogChanged(true)
    }

    fun hideExportImportDialog() {
        onShowExportImportDialogChanged(false)
    }
}