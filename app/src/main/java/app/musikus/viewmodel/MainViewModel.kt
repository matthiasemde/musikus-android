/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.dataStore
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.datastore.ThemeSelections
import app.musikus.repository.GoalRepository
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.shared.MultiFabState
import app.musikus.utils.getCurrTimestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.pow


data class MainMenuUiState(
    val show: Boolean,
    val showThemeSubMenu: Boolean,
)
data class MainUiState(
    val menuUiState: MainMenuUiState,
    val showExportImportDialog: Boolean,
    val multiFabState: MultiFabState,
)

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Database */
    private val database = MusikusDatabase.getInstance(application, ::prepopulateDatabase)

    /** Initialization */
    init {
        // Clean up soft-deleted items
        viewModelScope.launch {
            Log.d("Musikus", "Clean up soft-deleted items")
            SessionRepository(database).clean()
            GoalRepository(database).clean()
            LibraryRepository(database).clean()
        }
    }

    /** Repositories */
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore)
    private val libraryRepository = LibraryRepository(database)

    private fun prepopulateDatabase() {
        Log.d("MainViewModel", "prepopulateDatabase")
        viewModelScope.launch {
            listOf(
                LibraryFolderCreationAttributes(name = "Schupra"),
                LibraryFolderCreationAttributes(name = "Fagott"),
                LibraryFolderCreationAttributes(name = "Gesang"),
            ).forEach {
                libraryRepository.addFolder(it)
                Log.d("MainActivity", "Folder ${it.name} created")
                delay(1500) //make sure folders have different createdAt values
            }

            libraryRepository.folders.first().let { folders ->
                // populate the libraryItem table on first run
                listOf(
                    LibraryItemCreationAttributes(name = "Die Schöpfung", colorIndex = 0, libraryFolderId = Nullable(folders[0].id)),
                    LibraryItemCreationAttributes(name = "Beethoven Septett",colorIndex = 1,libraryFolderId = Nullable(folders[0].id)),
                    LibraryItemCreationAttributes(name = "Schostakowitsch 9.", colorIndex = 2, libraryFolderId = Nullable(folders[1].id)),
                    LibraryItemCreationAttributes(name = "Trauermarsch c-Moll", colorIndex = 3, libraryFolderId = Nullable(folders[1].id)),
                    LibraryItemCreationAttributes(name = "Adagio", colorIndex = 4, libraryFolderId = Nullable(folders[2].id)),
                    LibraryItemCreationAttributes(name = "Eine kleine Gigue", colorIndex = 5, libraryFolderId = Nullable(folders[2].id)),
                    LibraryItemCreationAttributes(name = "Andantino", colorIndex = 6),
                    LibraryItemCreationAttributes(name = "Klaviersonate", colorIndex = 7),
                    LibraryItemCreationAttributes(name = "Trauermarsch", colorIndex = 8),
                ).forEach {
                    libraryRepository.addItem(it)
                    Log.d("MainActivity", "LibraryItem ${it.name} created")
                    delay(1500) //make sure items have different createdAt values
                }
            }


            libraryRepository.items.first().let { items ->
                listOf(
                    GoalDescriptionCreationAttributes(
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = (1..5).random(),
                        periodUnit = GoalPeriodUnit.DAY,
                    ),
                    GoalDescriptionCreationAttributes(
                        type = GoalType.NON_SPECIFIC,
                        repeat = false,
                        periodInPeriodUnits = (1..5).random(),
                        periodUnit = GoalPeriodUnit.MONTH,
                    ),
                    GoalDescriptionCreationAttributes(
                        type = GoalType.ITEM_SPECIFIC,
                        repeat = false,
                        periodInPeriodUnits = (1..5).random(),
                        periodUnit = GoalPeriodUnit.WEEK,
                    ),
                    GoalDescriptionCreationAttributes(
                        type = GoalType.ITEM_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = (1..5).random(),
                        periodUnit = GoalPeriodUnit.DAY,
                    ),
                ).forEach {
                    Log.d("MainActivity", "GoalDescription ${it.type} created")
                    GoalRepository(database).add(
                        it,
                        if (it.type == GoalType.NON_SPECIFIC) null else listOf(items.random()),
                        (1..10).random() * 60
                    )
                    delay(1500)
                }

                (0..40).map { sessionNum ->
                    sessionNum to SessionCreationAttributes(
                        breakDuration = (5..20).random() * 60,
                        rating = (1..5).random(),
                        comment = "",
                    )
                }.forEach { (sessionNum, session) ->
                    SessionRepository(database).add(
                        session,
                        (1..(1..5).random()).map { SectionCreationAttributes(
                            libraryItemId = Nullable(items.random().id),
                            timestamp =
                                getCurrTimestamp() -
                                (
                                    ((sessionNum / 2) * 2) * // two sessions per day initially
                                    24 * 60 * 60 *
                                    1.05.pow(sessionNum.toDouble()) // exponential growth
                                ).toLong(),
                            duration = (5..20).random() * 60,
                        )}
                    )
                    delay(1000)
                }
            }
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