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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import de.practicetime.practicetime.dataStore
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.repository.LibraryRepository
import de.practicetime.practicetime.repository.UserPreferencesRepository
import de.practicetime.practicetime.shared.MultiFABState
import de.practicetime.practicetime.shared.SpinnerState
import kotlinx.coroutines.flow.map
import java.util.*

enum class LibraryMenuSelections {
}

enum class DialogMode {
    ADD,
    EDIT
}

class LibraryViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Initialization */

    /** Database */
    private val database = PTDatabase.getInstance(application)

    /** Repositories */
    private val libraryRepository = LibraryRepository("LibraryViewModel", database)
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore, application)

    init {
        Log.d("LibraryViewModel", "initialized")
    }

    // Folders
    val folders = libraryRepository.folders

    val folderSortMode = userPreferencesRepository.userPreferences.map { it.libraryFolderSortMode }
    val folderSortDirection = userPreferencesRepository.userPreferences.map { it.libraryFolderSortDirection }

    // Items
    val items = libraryRepository.items

    val itemSortMode = userPreferencesRepository.userPreferences.map { it.libraryItemSortMode }
    val itemSortDirection = userPreferencesRepository.userPreferences.map { it.libraryItemSortDirection }

    // Menu
    var showFolderSortModeMenu = mutableStateOf(false)
    var showItemSortModeMenu = mutableStateOf(false)

    val activeFolder = mutableStateOf<LibraryFolder?>(null)

    // Folder dialog
    var showFolderDialog = mutableStateOf(false)
    var editableFolder = mutableStateOf<LibraryFolder?>(null)
    var folderDialogMode = mutableStateOf(DialogMode.ADD)
    var folderDialogName = mutableStateOf("")

    fun clearFolderDialog() {
        showFolderDialog.value = false
        editableFolder.value = null
        folderDialogName.value = ""
    }

    // Item dialog
    var showItemDialog = mutableStateOf(false)
    var editableItem = mutableStateOf<LibraryItem?>(null)
    var itemDialogMode = mutableStateOf(DialogMode.ADD)
    var itemDialogName = mutableStateOf("")
    var itemDialogColorIndex = mutableStateOf(0)
    var itemDialogFolderId = mutableStateOf<UUID?>(null)
    var itemDialogFolderSelectorExpanded = mutableStateOf(SpinnerState.COLLAPSED)

    fun clearItemDialog() {
        showItemDialog.value = false
        editableItem.value = null
        itemDialogName.value = ""
        itemDialogColorIndex.value = 0
        itemDialogFolderId.value = null
        itemDialogFolderSelectorExpanded.value = SpinnerState.COLLAPSED
    }

    fun onItemDialogConfirmed() {
        when(itemDialogMode.value) {
            DialogMode.ADD -> {
                libraryRepository.addItem(
                    LibraryItem(
                        name = itemDialogName.value,
                        colorIndex = itemDialogColorIndex.value,
                        libraryFolderId = itemDialogFolderId.value
                    )
                )
            }
            DialogMode.EDIT -> {
                editableItem.value?.apply {
                    name = itemDialogName.value
                    colorIndex = itemDialogColorIndex.value
                    libraryFolderId = itemDialogFolderId.value
                    libraryRepository.editItem(this)
                }
            }
        }
    }

    fun onFolderDialogConfirmed() {
        when(folderDialogMode.value) {
            DialogMode.ADD -> {
                libraryRepository.addFolder(
                    LibraryFolder(
                        name = folderDialogName.value,
                    )
                )
            }
            DialogMode.EDIT -> {
                editableFolder.value?.apply {
                    name = folderDialogName.value
                    libraryRepository.editFolder(this)
                }
            }
        }
    }

    // Multi FAB
    var multiFABState = mutableStateOf(MultiFABState.COLLAPSED)

    // Action mode
    var actionMode = mutableStateOf(false)

    val selectedItemIds = mutableStateListOf<UUID>()
    val selectedFolderIds = mutableStateListOf<UUID>()

    fun clearActionMode() {
        selectedItemIds.clear()
        selectedFolderIds.clear()
        actionMode.value = false
    }
}
