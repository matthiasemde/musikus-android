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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.practicetime.practicetime.dataStore
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.datastore.LibraryFolderSortMode
import de.practicetime.practicetime.datastore.LibraryItemSortMode
import de.practicetime.practicetime.datastore.SortDirection
import de.practicetime.practicetime.repository.LibraryRepository
import de.practicetime.practicetime.repository.UserPreferencesRepository
import de.practicetime.practicetime.shared.MultiFABState
import de.practicetime.practicetime.shared.SpinnerState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch
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

    val userPreferences = userPreferencesRepository.userPreferences.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    // Folders
    val folders = libraryRepository.folders.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = listOf()
    )

    val folderSortMode = userPreferencesRepository.userPreferences.map {
        it.libraryFolderSortMode
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryFolderSortMode.defaultValue
    )
    val folderSortDirection = userPreferencesRepository.userPreferences.map {
        it.libraryFolderSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortDirection.defaultValue
    )

    fun onFolderSortModeSelected(sortMode: LibraryFolderSortMode) {
        showFolderSortModeMenu.value = false
        viewModelScope.launch {
            userPreferencesRepository.updateLibraryFolderSortMode(sortMode)
        }
    }

    val sortedFolders = folders.combine(userPreferences) { folders, preferences ->
        if(preferences == null) return@combine folders
        libraryRepository.sortFolders(
            folders = folders,
            mode = preferences.libraryFolderSortMode,
            direction = preferences.libraryFolderSortDirection
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Items
    private val items = libraryRepository.items.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sortedItems = items.combine(userPreferences) { items, preferences ->
        if(preferences == null) return@combine items
        libraryRepository.sortItems(
            items = items,
            mode = preferences.libraryItemSortMode,
            direction = preferences.libraryItemSortDirection
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val itemSortMode = userPreferencesRepository.userPreferences.map {
        it.libraryItemSortMode
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryItemSortMode.defaultValue
    )

    val itemSortDirection = userPreferencesRepository.userPreferences.map {
        it.libraryItemSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortDirection.defaultValue
    )

    fun onItemSortModeSelected(selection: LibraryItemSortMode) {
        showItemSortModeMenu.value = false
        viewModelScope.launch {
            userPreferencesRepository.updateLibraryItemSortMode(selection)
        }
    }

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

    private var editableItem: LibraryItem? = null

    var itemDialogMode = mutableStateOf(DialogMode.ADD)
    var itemDialogName = mutableStateOf("")
    var itemDialogColorIndex = mutableStateOf(0)
    var itemDialogFolderId = mutableStateOf<UUID?>(null)
    var itemDialogFolderSelectorExpanded = mutableStateOf(SpinnerState.COLLAPSED)

    fun clearItemDialog() {
        showItemDialog.value = false
        editableItem = null
        itemDialogName.value = ""
        itemDialogColorIndex.value = 0
        itemDialogFolderId.value = null
        itemDialogFolderSelectorExpanded.value = SpinnerState.COLLAPSED
    }

    fun onItemDialogConfirmed() {
        viewModelScope.launch {
            when (itemDialogMode.value) {
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
                    editableItem?.let {
                        libraryRepository.editItem(
                            item = it,
                            newName = itemDialogName.value,
                            newColorIndex = itemDialogColorIndex.value,
                            newFolderId = itemDialogFolderId.value
                        )
                    }
                }
            }
        }
    }

    fun onFolderDialogConfirmed() {
        viewModelScope.launch {
            when (folderDialogMode.value) {
                DialogMode.ADD -> {
                    libraryRepository.addFolder(
                        LibraryFolder(name = folderDialogName.value)
                    )
                }
                DialogMode.EDIT -> {
                    editableFolder.value?.let {
                        libraryRepository.editFolder(
                            folder = it,
                            newName = folderDialogName.value
                        )
                    }
                }
            }
        }
    }

    // Hint
    val showHint = folders.combine(items) { folders, items ->
        Log.d("LibraryViewModel", "showHint: folders = $folders, items = $items")
        folders.isEmpty() && items.isEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = true
    )

    // Multi FAB
    var multiFABState = mutableStateOf(MultiFABState.COLLAPSED)

    // Action mode

    private val _selectedFolders = MutableStateFlow<Set<LibraryFolder>>(emptySet())
    val selectedFolders = _selectedFolders.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<LibraryItem>>(emptySet())
    val selectedItems = _selectedItems.asStateFlow()

    var actionMode = _selectedFolders.combine(_selectedItems) { selectedFolders, selectedItems ->
        selectedFolders.isNotEmpty() || selectedItems.isNotEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = false
    )

    fun onFolderClicked(
        folder: LibraryFolder,
        longClick: Boolean = false
    ) {
        if (longClick) {
            _selectedFolders.update { it + folder }
            return
        }

        // Short Click
        if(!actionMode.value) {
            activeFolder.value = folder
        } else {
            if(_selectedFolders.value.contains(folder)) {
                _selectedFolders.update { it - folder }
            } else {
                _selectedFolders.update { it + folder }
            }
        }
    }

    fun onItemClicked(
        item: LibraryItem,
        longClick: Boolean = false
    ) {
        if (longClick) {
            _selectedItems.update { it + item }
            return
        }

        // Short Click
        if(!actionMode.value) {
            editableItem = item
            itemDialogMode.value = DialogMode.EDIT
            itemDialogName.value = item.name
            itemDialogColorIndex.value = item.colorIndex
            itemDialogFolderId.value = item.libraryFolderId
            showItemDialog.value = true
        } else {
            if(_selectedItems.value.contains(item)) {
                _selectedItems.update { it - item }
            } else {
                _selectedItems.update { it + item }
            }
        }
    }

    fun onDeleteAction() {
        viewModelScope.launch {
            libraryRepository.deleteFolders(_selectedFolders.value)
            libraryRepository.archiveItems(_selectedItems.value)
            clearActionMode()
        }
    }

    fun onEditAction() {
//        assert(_selectedFolders.value.size + _selectedItems.value.size == 1) // TODO: DO we need this?
        _selectedFolders.value.firstOrNull()?.let {
            editableFolder.value = it
            folderDialogMode.value = DialogMode.EDIT
            folderDialogName.value = it.name
            showFolderDialog.value = true
        } ?: _selectedItems.value.firstOrNull()?.let {
            editableItem = it
            itemDialogMode.value = DialogMode.EDIT
            itemDialogName.value = it.name
            itemDialogColorIndex.value = it.colorIndex
            itemDialogFolderId.value = it.libraryFolderId
            showItemDialog.value = true
        }
        clearActionMode()
    }

    fun clearActionMode() {
        _selectedItems.update { emptySet() }
        _selectedFolders.update { emptySet() }
    }
}
