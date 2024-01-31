/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.Nullable
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.usecase.library.LibraryUseCases
import app.musikus.usecase.userpreferences.UserPreferencesUseCases
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class DialogMode {
    ADD,
    EDIT
}

data class LibraryFolderEditData(
    val name: String,
)

data class LibraryItemEditData(
    val name: String,
    val colorIndex: Int,
    val folderId: UUID?,
)

data class LibraryDialogState(
    val folderDialogUiState: LibraryFolderDialogUiState?,
    val itemDialogUiState: LibraryItemDialogUiState?,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    private val userPreferencesUseCases: UserPreferencesUseCases,
) : ViewModel() {


    /** Private variables */
    private var _foldersCache = emptyList<LibraryFolder>()
    private var _itemsCache = emptyList<LibraryItem>()

    /** Imported flows */

    private val foldersWithItems = libraryUseCases.getSortedFolders().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val itemSortInfo = userPreferencesUseCases.getItemSortInfo().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortInfo(
            mode = LibraryItemSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    private val folderSortInfo = userPreferencesUseCases.getFolderSortInfo().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortInfo(
            mode = LibraryFolderSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
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

    // Action mode
    private val _selectedFolders = MutableStateFlow<Set<LibraryFolder>>(emptySet())
    private val _selectedItems = MutableStateFlow<Set<LibraryItem>>(emptySet())


    /** Combining imported and own flows  */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val items = _activeFolder.flatMapLatest { activeFolder ->
        libraryUseCases.getSortedItems(
            folderId = Nullable(activeFolder?.id)
        ).stateIn(
            scope = viewModelScope,
            started = WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Composing the Ui state
     */
    private val topBarUiState = _activeFolder.map { activeFolder ->
        val title = activeFolder?.name ?: "Library"
        val showBackButton = activeFolder != null

        LibraryTopBarUiState(
            title = title,
            showBackButton = showBackButton,
        )
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
        )
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
        folderSortInfo,
    ) { showMenu, sortInfo ->
        LibraryFoldersSortMenuUiState(
            show = showMenu,
            mode = sortInfo.mode,
            direction = sortInfo.direction,
        )
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
        foldersWithItems,
        _selectedFolders,
        foldersSortMenuUiState,
    ) { foldersWithItems, selectedFolders, sortMenuUiState ->
        if(foldersWithItems.isEmpty()) return@combine null

        LibraryFoldersUiState(
            foldersWithItems = foldersWithItems,
            selectedFolders = selectedFolders,
            sortMenuUiState = sortMenuUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val itemsSortMenuUiState = combine(
        _showItemSortMenu,
        itemSortInfo,
    ) { showMenu, sortInfo ->
        LibraryItemsSortMenuUiState(
            show = showMenu,
            mode = sortInfo.mode,
            direction = sortInfo.direction,
        )
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
        items,
        _selectedItems,
        itemsSortMenuUiState,
    ) { items, selectedItems, sortMenuUiState ->
        if(items.isEmpty()) return@combine null

        LibraryItemsUiState(
            items = items,
            selectedItems = selectedItems,
            sortMenuUiState = sortMenuUiState,
        )
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
        )
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val itemDialogUiState = combine(
        _itemEditData,
        _itemToEdit,
        foldersWithItems,
        _folderSelectorExpanded,
    ) { editData, itemToEdit, foldersWithItems, isFolderSelectorExpanded ->
        if(editData == null) return@combine null
        val confirmButtonEnabled = editData.name.isNotBlank()

        LibraryItemDialogUiState(
            mode = if (itemToEdit == null) DialogMode.ADD else DialogMode.EDIT,
            itemData = editData,
            folders = foldersWithItems.map { it.folder },
            isFolderSelectorExpanded = isFolderSelectorExpanded,
            confirmButtonEnabled = confirmButtonEnabled,
            itemToEdit = itemToEdit,
        )
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
        )
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryFabUiState(
            activeFolder = null,
        )
    )

    val uiState = combine(
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
        )
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
        if(!uiState.value.actionModeUiState.isActionMode) {
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
        if(!uiState.value.actionModeUiState.isActionMode) {
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
            _foldersCache = _selectedFolders.value.toList()
            _itemsCache = _selectedItems.value.toList()

            libraryUseCases.deleteFolders(_foldersCache)
            libraryUseCases.deleteItems(_itemsCache)

            clearActionMode()
        }
    }

    fun onRestoreAction() {
        viewModelScope.launch {
            libraryUseCases.restoreFolders(_foldersCache)
            libraryUseCases.restoreItems(_itemsCache)
        }
    }
    fun onEditAction() {
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
                libraryUseCases.editFolder(
                    id = it.id,
                    updateAttributes = LibraryFolderUpdateAttributes(
                        name = folderData.name
                    )
                )
            } ?: libraryUseCases.addFolder(
                LibraryFolderCreationAttributes(name = folderData.name)
            )
            clearFolderDialog()
        }
    }

    fun onItemDialogConfirmed() {
        viewModelScope.launch {
            val itemData = _itemEditData.value ?: return@launch
            _itemToEdit.value?.let {
                libraryUseCases.editItem(
                    id = it.id,
                    LibraryItemUpdateAttributes (
                        name = itemData.name,
                        colorIndex = itemData.colorIndex,
                        libraryFolderId = Nullable(itemData.folderId),
                    )
                )
            } ?: libraryUseCases.addItem(
                LibraryItemCreationAttributes(
                    name = itemData.name,
                    colorIndex = itemData.colorIndex,
                    libraryFolderId = Nullable(itemData.folderId)
                )
            )
            clearItemDialog()
        }
    }

    fun clearActionMode() {
        _selectedItems.update { emptySet() }
        _selectedFolders.update { emptySet() }
    }

    fun onItemSortModeSelected(selection: LibraryItemSortMode) {
        _showItemSortMenu.update { false }
        viewModelScope.launch {
            userPreferencesUseCases.selectItemSortMode(selection)
        }
    }

    fun onFolderSortModeSelected(selection: LibraryFolderSortMode) {
        _showFolderSortMenu.update { false }
        viewModelScope.launch {
            userPreferencesUseCases.selectFolderSortMode(selection)
        }
    }
}
