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
import de.practicetime.practicetime.shared.TopBarUiState
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

data class LibraryFolderEditData(
    val name: String,
)

data class LibraryFolderWithItemCount(
    val folder: LibraryFolder,
    val itemCount: Int,
)

data class LibraryItemEditData(
    val name: String,
    val colorIndex: Int,
    val folderId: UUID?,
)

data class LibraryTopBarUiState(
    override val title: String,
    override val showBackButton: Boolean,
) : TopBarUiState

data class LibraryActionModeUiState(
    val isActionMode: Boolean,
    val numberOfSelections: Int,
)

data class LibraryFoldersSortMenuUiState(
    val show: Boolean,

    val mode: LibraryFolderSortMode,
    val direction: SortDirection,
)

data class LibraryFoldersUiState(
    val foldersWithItemCount: List<LibraryFolderWithItemCount>,
    val selectedFolders: Set<LibraryFolder>,

    val sortMenuUiState: LibraryFoldersSortMenuUiState
)

data class LibraryItemsSortMenuUiState(
    val show: Boolean,

    val mode: LibraryItemSortMode,
    val direction: SortDirection,
)

data class LibraryItemsUiState(
    val items: List<LibraryItem>,
    val selectedItems: Set<LibraryItem>,

    val sortMenuUiState: LibraryItemsSortMenuUiState
)

data class LibraryContentUiState(
    val foldersUiState: LibraryFoldersUiState?,
    val itemsUiState: LibraryItemsUiState?,

    val showHint: Boolean,
)

data class LibraryFolderDialogUiState(
    val mode: DialogMode,
    val folderData: LibraryFolderEditData,
    val confirmButtonEnabled: Boolean,
    val folderToEdit: LibraryFolder?,
)

data class LibraryItemDialogUiState(
    val mode: DialogMode,
    val itemData: LibraryItemEditData,
    val folders : List<LibraryFolder>,
    val isFolderSelectorExpanded: Boolean,
    val confirmButtonEnabled: Boolean,
    val itemToEdit: LibraryItem?,
)

data class LibraryDialogState(
    val folderDialogUiState: LibraryFolderDialogUiState?,
    val itemDialogUiState: LibraryItemDialogUiState?,
)

data class LibraryFabUiState(
    val activeFolder: LibraryFolder?,
)

data class LibraryUiState (
    val topBarUiState: LibraryTopBarUiState,
    val actionModeUiState: LibraryActionModeUiState,
    val contentUiState: LibraryContentUiState,
    val dialogUiState: LibraryDialogState,
    val fabUiState: LibraryFabUiState,
)

class LibraryViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Database */
    private val database = PTDatabase.getInstance(application)


    /** Repositories */
    private val libraryRepository = LibraryRepository(database)
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore)


    /** Imported flows */

    private val foldersSortMode = userPreferencesRepository.userPreferences.map {
      it.libraryFolderSortMode
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryFolderSortMode.DEFAULT
    )

    private val foldersSortDirection = userPreferencesRepository.userPreferences.map {
      it.libraryFolderSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortDirection.DEFAULT
    )

    private val itemsSortMode = userPreferencesRepository.userPreferences.map {
      it.libraryItemSortMode
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryItemSortMode.DEFAULT
    )

    private val itemsSortDirection = userPreferencesRepository.userPreferences.map {
      it.libraryItemSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortDirection.DEFAULT
    )

    private val folders = libraryRepository.folders.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val items = libraryRepository.items.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val sortedFoldersWithItemCount = combine(
        folders,
        foldersSortMode,
        foldersSortDirection,
        items
    ) { folders, sortMode, sortDirection, items ->
        val sortedFolders = libraryRepository.sortFolders(
            folders = folders,
            mode = sortMode,
            direction = sortDirection
        )

        sortedFolders.map { folder ->
            val itemCount = items.count { it.libraryFolderId == folder.id }
            LibraryFolderWithItemCount(folder, itemCount)
        }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )


    /** Own state flows */

    // Menu
    private val _showFolderSortMenu = MutableStateFlow(false)
    private val _showItemSortMenu = MutableStateFlow(false)

    private val _activeFolder = MutableStateFlow<LibraryFolder?>(null)

    // Folder dialog
    private val _folderEditData = MutableStateFlow<LibraryFolderEditData?>(null)
    private val _folderToEdit = MutableStateFlow<LibraryFolder?>(null)

    // Item dialog
    private val _itemEditData = MutableStateFlow<LibraryItemEditData?>(null)
    private val _itemToEdit = MutableStateFlow<LibraryItem?>(null)

    private val _folderSelectorExpanded = MutableStateFlow(false)

    // Multi FAB TODO
    var multiFABState = mutableStateOf(MultiFABState.COLLAPSED)

    // Action mode
    private val _selectedFolders = MutableStateFlow<Set<LibraryFolder>>(emptySet())
    private val _selectedItems = MutableStateFlow<Set<LibraryItem>>(emptySet())


    /** Combining imported and own flows */

    private val sortedItems = combine(
        items,
        itemsSortMode,
        itemsSortDirection
    ) { items, sortMode, sortDirection ->
        libraryRepository.sortItems(
            items = items,
            mode = sortMode,
            direction = sortDirection
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val sortedItemsInActiveFolder = combine(
        sortedItems,
        _activeFolder,
    ) { sortedItems, activeFolder ->
        sortedItems.filter { it.libraryFolderId == activeFolder?.id }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Composing the Ui state
     */
    private val topBarUiState = _activeFolder.map { activeFolder ->
        val title = activeFolder?.name ?: "Library"
        val showBackButton = activeFolder != null

        LibraryTopBarUiState(
            title = title,
            showBackButton = showBackButton,
        ).also { Log.d("LibraryViewModel", "topBarUiState updated") }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryTopBarUiState(
            title = "Library",
            showBackButton = false,
        )
    )

    private val actionModeUiState = combine(
        _selectedFolders,
        _selectedItems,
    ) { selectedFolders, selectedItems ->
        LibraryActionModeUiState(
            isActionMode = selectedFolders.isNotEmpty() || selectedItems.isNotEmpty(),
            numberOfSelections = selectedFolders.size + selectedItems.size,
        ).also { Log.d("LibraryViewModel", "actionModeUiState updated") }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryActionModeUiState(
            isActionMode = false,
            numberOfSelections = 0,
        )
    )

    private val foldersSortMenuUiState = combine(
        _showFolderSortMenu,
        foldersSortMode,
        foldersSortDirection,
    ) { showMenu, sortMode, sortDirection ->
        LibraryFoldersSortMenuUiState(
            show = showMenu,
            mode = sortMode,
            direction = sortDirection,
        ).also { Log.d("LibraryViewModel", "foldersSortMenuUiState updated") }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryFoldersSortMenuUiState(
            show = false,
            mode = LibraryFolderSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    private val foldersUiState = combine(
        sortedFoldersWithItemCount,
        _selectedFolders,
        foldersSortMenuUiState,
    ) { sortedFoldersWithItemCount, selectedFolders, sortMenuUiState ->
        if(sortedFoldersWithItemCount.isEmpty()) return@combine null

        LibraryFoldersUiState(
            foldersWithItemCount = sortedFoldersWithItemCount,
            selectedFolders = selectedFolders,
            sortMenuUiState = sortMenuUiState,
        ).also { Log.d("LibraryViewModel", "foldersUiState updated") }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val itemsSortMenuUiState = combine(
        _showItemSortMenu,
        itemsSortMode,
        itemsSortDirection,
    ) { showMenu, sortMode, sortDirection ->
        LibraryItemsSortMenuUiState(
            show = showMenu,
            mode = sortMode,
            direction = sortDirection,
        ).also { Log.d("LibraryViewModel", "itemsSortMenuUiState updated") }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryItemsSortMenuUiState(
            show = false,
            mode = LibraryItemSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    private val itemsUiState = combine(
        sortedItemsInActiveFolder,
        _selectedItems,
        itemsSortMenuUiState,
    ) { sortedItemsInActiveFolder, selectedItems, sortMenuUiState ->
        if(sortedItemsInActiveFolder.isEmpty()) return@combine null

        LibraryItemsUiState(
            items = sortedItemsInActiveFolder,
            selectedItems = selectedItems,
            sortMenuUiState = sortMenuUiState,
        ).also { Log.d("LibraryViewModel", "itemsUiState updated") }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val contentUiState = combine(
        foldersUiState,
        _activeFolder,
        itemsUiState,
    ) { foldersUiState, activeFolder, itemsUiState ->
        LibraryContentUiState(
            foldersUiState = if (activeFolder == null) foldersUiState else null,
            itemsUiState = itemsUiState,
            showHint = foldersUiState == null && itemsUiState == null,
        ).also { Log.d("LibraryViewModel", "contentUiState updated") }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryContentUiState(
            foldersUiState = foldersUiState.value,
            itemsUiState = itemsUiState.value,
            showHint = true,
        )
    )

    private val folderDialogUiState = combine(
        _folderEditData,
        _folderToEdit,
    ) { editData, folderToEdit ->
        if(editData == null) return@combine null
        val confirmButtonEnabled = editData.name.isNotBlank()

        LibraryFolderDialogUiState(
            mode = if (folderToEdit == null) DialogMode.ADD else DialogMode.EDIT,
            folderData = editData,
            confirmButtonEnabled = confirmButtonEnabled,
            folderToEdit = folderToEdit,
        ).also { Log.d("LibraryViewModel", "folderDialogUiState updated") }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val itemDialogUiState = combine(
        _itemEditData,
        _itemToEdit,
        folders,
        _folderSelectorExpanded,
    ) { editData, itemToEdit, folders, isFolderSelectorExpanded ->
        if(editData == null) return@combine null
        val confirmButtonEnabled = editData.name.isNotBlank()

        LibraryItemDialogUiState(
            mode = if (itemToEdit == null) DialogMode.ADD else DialogMode.EDIT,
            itemData = editData,
            folders = folders,
            isFolderSelectorExpanded = isFolderSelectorExpanded,
            confirmButtonEnabled = confirmButtonEnabled,
            itemToEdit = itemToEdit,
        ).also {
            Log.d("LibraryViewModel", "itemDialogUiState updated")
        }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val dialogUiState = combine(
        folderDialogUiState,
        itemDialogUiState,
    ) { folderDialogUiState, itemDialogUiState ->
        assert (folderDialogUiState == null || itemDialogUiState == null)
        LibraryDialogState(
            folderDialogUiState = folderDialogUiState,
            itemDialogUiState = itemDialogUiState,
        ).also {
            Log.d("LibraryViewModel", "dialogUiState updated")
        }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryDialogState(
            folderDialogUiState = folderDialogUiState.value,
            itemDialogUiState = itemDialogUiState.value,
        )
    )

    private val fabUiState = _activeFolder.map { activeFolder ->
        LibraryFabUiState(
            activeFolder = activeFolder,
        ).also {
            Log.d("LibraryViewModel", "fabUiState updated")
        }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryFabUiState(
            activeFolder = null,
        )
    )

    val libraryUiState = combine(
        topBarUiState,
        actionModeUiState,
        contentUiState,
        dialogUiState,
        fabUiState,
    ) { topBarUiState, actionModeUiState, contentUiState, dialogUiState, fabUiState ->
        LibraryUiState(
            topBarUiState = topBarUiState,
            actionModeUiState = actionModeUiState,
            contentUiState = contentUiState,
            dialogUiState = dialogUiState,
            fabUiState = fabUiState,
        ).also {
            Log.d("LibraryViewModel", "UiState updated")
        }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryUiState(
            topBarUiState = topBarUiState.value,
            actionModeUiState = actionModeUiState.value,
            contentUiState = contentUiState.value,
            dialogUiState = dialogUiState.value,
            fabUiState = fabUiState.value,
        )
    )

    /**
     * Mutators
     */

    fun onTopBarBackPressed() {
        _activeFolder.update { null }
    }

    fun onFolderClicked(
        folder: LibraryFolder,
        longClick: Boolean = false
    ) {
        if (longClick) {
            _selectedFolders.update { it + folder }
            return
        }

        // Short Click
        if(!libraryUiState.value.actionModeUiState.isActionMode) {
            _activeFolder.update { folder }
        } else {
            if(_selectedFolders.value.contains(folder)) {
                _selectedFolders.update { it - folder }
            } else {
                _selectedFolders.update { it + folder }
            }
        }
    }

    fun onFolderSortMenuChanged(show: Boolean) {
        _showFolderSortMenu.update { show }
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
        if(!libraryUiState.value.actionModeUiState.isActionMode) {
            _itemToEdit.update { item }
            _itemEditData.update {
                LibraryItemEditData(
                    name = item.name,
                    colorIndex = item.colorIndex,
                    folderId = item.libraryFolderId
                )
            }
        } else {
            if(_selectedItems.value.contains(item)) {
                _selectedItems.update { it - item }
            } else {
                _selectedItems.update { it + item }
            }
        }
    }

    fun onItemSortMenuChanged(show: Boolean) {
        _showItemSortMenu.update { show }
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
        _selectedFolders.value.firstOrNull()?.let { folderToEdit ->
            _folderToEdit.update { folderToEdit }
            _folderEditData.update {
                LibraryFolderEditData(
                    name = folderToEdit.name
                )
            }
        } ?: _selectedItems.value.firstOrNull()?.let { itemToEdit ->
            _itemToEdit.update { itemToEdit }
            _itemEditData.update {
                LibraryItemEditData(
                    name = itemToEdit.name,
                    colorIndex = itemToEdit.colorIndex,
                    folderId = itemToEdit.libraryFolderId
                )
            }
        }
        clearActionMode()
    }

    fun showFolderDialog() {
        _folderEditData.update {
            LibraryFolderEditData(
                name = ""
            )
        }
    }

    fun showItemDialog(folderId: UUID? = null) {
        _itemEditData.update {
            LibraryItemEditData(
                name = "",
                colorIndex = (Math.random() * 10).toInt(),
                folderId = folderId
            )
        }
    }

    fun onFolderDialogNameChanged(newName: String) {
        _folderEditData.update { it?.copy(name = newName) }
    }

    fun onItemDialogNameChanged(newName: String) {
        _itemEditData.update { it?.copy(name = newName) }
    }

    fun onItemDialogColorIndexChanged(newColorIndex: Int) {
        _itemEditData.update { it?.copy(colorIndex = newColorIndex) }
    }

    fun onItemDialogFolderIdChanged(newFolderId: UUID?) {
        _itemEditData.update { it?.copy(folderId = newFolderId) }
        _folderSelectorExpanded.update { false }
    }

    fun onFolderSelectorExpandedChanged(isExpanded: Boolean) {
        _folderSelectorExpanded.update { isExpanded }
    }

    fun clearFolderDialog() {
        _folderToEdit.update { null }
        _folderEditData.update { null }
    }

    fun clearItemDialog() {
        _itemToEdit.update { null }
        _itemEditData.update { null }
        _folderSelectorExpanded.update { false }
    }

    fun onFolderDialogConfirmed() {
        viewModelScope.launch {
            val folderData = _folderEditData.value ?: return@launch
            _folderToEdit.value?.let {
                libraryRepository.editFolder(
                    folder = it,
                    newName = folderData.name
                )
            } ?: libraryRepository.addFolder(
                LibraryFolder(name = folderData.name)
            )
            clearFolderDialog()
        }
    }

    fun onItemDialogConfirmed() {
        viewModelScope.launch {
            val itemData = _itemEditData.value ?: return@launch
            _itemToEdit.value?.let {
                libraryRepository.editItem(
                    item = it,
                    newName = itemData.name,
                    newColorIndex = itemData.colorIndex,
                    newFolderId = itemData.folderId
                )
            } ?: libraryRepository.addItem(
                LibraryItem(
                    name = itemData.name,
                    colorIndex = itemData.colorIndex,
                    libraryFolderId = itemData.folderId
                )
            )
            clearItemDialog()
        }
    }

    fun clearActionMode() {
        _selectedItems.update { emptySet() }
        _selectedFolders.update { emptySet() }
    }

    fun onFolderSortModeSelected(sortMode: LibraryFolderSortMode) {
        _showFolderSortMenu.update { false }
        viewModelScope.launch {
            userPreferencesRepository.updateLibraryFolderSortMode(sortMode)
        }
    }

    fun onItemSortModeSelected(selection: LibraryItemSortMode) {
        _showItemSortMenu.update { false }
        viewModelScope.launch {
            userPreferencesRepository.updateLibraryItemSortMode(selection)
        }
    }
}
