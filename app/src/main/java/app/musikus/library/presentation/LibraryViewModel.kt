/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.Nullable
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
    private val userPreferencesUseCases: UserPreferencesUseCases,
) : ViewModel() {


    /** Private variables */
    private var _foldersCache = emptyList<UUID>()
    private var _itemsCache = emptyList<UUID>()

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
    private val _folderToEditId = MutableStateFlow<UUID?>(null)

    // Item dialog
    private val _itemEditData = MutableStateFlow<LibraryItemEditData?>(null)
    private val _itemToEditId = MutableStateFlow<UUID?>(null)

    // Action mode
    private val _selectedFolderIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val _selectedItemIds = MutableStateFlow<Set<UUID>>(emptySet())

    // Delete dialog
    private val _showDeleteDialog = MutableStateFlow(false)


    /** Combining imported and own flows  */

    private val items = _activeFolder.flatMapLatest { activeFolder ->
        libraryUseCases.getSortedItems(
            folderId = Nullable(activeFolder?.id)
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val lastPracticedDates = items.flatMapLatest { items ->
        libraryUseCases.getLastPracticedDate(items)
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyMap()
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
        _selectedFolderIds,
        _selectedItemIds,
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
            show = _showFolderSortMenu.value,
            mode = LibraryFolderSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    private val foldersUiState = combine(
        foldersWithItems,
        _selectedFolderIds,
        foldersSortMenuUiState,
    ) { foldersWithItems, selectedFolders, sortMenuUiState ->
        if(foldersWithItems.isEmpty()) return@combine null

        LibraryFoldersUiState(
            foldersWithItems = foldersWithItems,
            selectedFolderIds = selectedFolders,
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
        lastPracticedDates,
        _selectedItemIds,
        itemsSortMenuUiState,
    ) { items, lastPracticedDates, selectedItems, sortMenuUiState ->
        if(items.isEmpty()) return@combine null

        LibraryItemsUiState(
            itemsWithLastPracticedDate = items.map { item ->
                item to lastPracticedDates[item.id]
            },
            selectedItemIds = selectedItems,
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
        _folderToEditId,
    ) { editData, folderToEditId ->
        if(editData == null) return@combine null
        val confirmButtonEnabled = editData.name.isNotBlank()

        LibraryFolderDialogUiState(
            mode = if (folderToEditId == null) DialogMode.ADD else DialogMode.EDIT,
            folderData = editData,
            confirmButtonEnabled = confirmButtonEnabled,
            folderToEditId = folderToEditId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val itemDialogUiState = combine(
        _itemEditData,
        _itemToEditId,
        foldersWithItems,
    ) { editData, itemToEditId, foldersWithItems ->
        if(editData == null) return@combine null
        val confirmButtonEnabled = editData.name.isNotBlank()

        LibraryLibraryItemDialogUiState(
            mode = if (itemToEditId == null) DialogMode.ADD else DialogMode.EDIT,
            itemData = editData,
            folders = foldersWithItems.map { it.folder },
            isConfirmButtonEnabled = confirmButtonEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val deleteDialogUiState = combine(
        _showDeleteDialog,
        _selectedFolderIds,
        _selectedItemIds,
    ) { showDeleteDialog, selectedFolders, selectedItems ->
        if (!showDeleteDialog) return@combine null

        LibraryDeleteDialogUiState(
            numberOfSelectedFolders = selectedFolders.size,
            numberOfSelectedItems = selectedItems.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    private val dialogUiState = combine(
        folderDialogUiState,
        itemDialogUiState,
        deleteDialogUiState
    ) { folderDialogUiState, itemDialogUiState, deleteDialogUiState ->
        assert (folderDialogUiState == null || itemDialogUiState == null)
        LibraryDialogUiState(
            folderDialogUiState = folderDialogUiState,
            itemDialogUiState = itemDialogUiState,
            deleteDialogUiState = deleteDialogUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryDialogUiState(
            folderDialogUiState = folderDialogUiState.value,
            itemDialogUiState = itemDialogUiState.value,
            deleteDialogUiState = deleteDialogUiState.value,
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

    fun onUiEvent(event: LibraryUiEvent) {
        when(event) {
            is LibraryUiEvent.BackButtonPressed -> onTopBarBackPressed()
            is LibraryUiEvent.FolderPressed -> onFolderClicked(event.folder, event.longClick)
            is LibraryUiEvent.FolderSortMenuPressed -> onFolderSortMenuChanged(_showFolderSortMenu.value.not())
            is LibraryUiEvent.FolderSortModeSelected -> onFolderSortModeSelected(event.mode)
            is LibraryUiEvent.ItemPressed -> onItemClicked(event.item, event.longClick)
            is LibraryUiEvent.ItemSortMenuPressed -> onItemSortMenuChanged(_showItemSortMenu.value.not())
            is LibraryUiEvent.ItemSortModeSelected -> onItemSortModeSelected(event.mode)
            is LibraryUiEvent.DeleteButtonPressed -> _showDeleteDialog.update { true }
            is LibraryUiEvent.DeleteDialogDismissed -> {
                _showDeleteDialog.update { false }
                clearActionMode()
            }
            is LibraryUiEvent.DeleteDialogConfirmed -> {
                _showDeleteDialog.update { false }
                onDeleteAction()
            }
            is LibraryUiEvent.RestoreButtonPressed -> onRestoreAction()
            is LibraryUiEvent.EditButtonPressed -> onEditAction()
            is LibraryUiEvent.AddFolderButtonPressed -> showFolderDialog()
            is LibraryUiEvent.AddItemButtonPressed -> showItemDialog()
            is LibraryUiEvent.FolderDialogNameChanged -> onFolderDialogNameChanged(event.name)
            is LibraryUiEvent.FolderDialogConfirmed -> onFolderDialogConfirmed()
            is LibraryUiEvent.FolderDialogDismissed -> clearFolderDialog()
            is LibraryUiEvent.ItemDialogUiEvent -> {
                when(val dialogEvent = event.dialogEvent) {
                    is LibraryItemDialogUiEvent.NameChanged -> onItemDialogNameChanged(dialogEvent.name)
                    is LibraryItemDialogUiEvent.ColorIndexChanged -> onItemDialogColorIndexChanged(dialogEvent.colorIndex)
                    is LibraryItemDialogUiEvent.FolderIdChanged -> onItemDialogFolderIdChanged(dialogEvent.folderId)
                    is LibraryItemDialogUiEvent.Confirmed -> onItemDialogConfirmed()
                    is LibraryItemDialogUiEvent.Dismissed -> clearItemDialog()
                }
            }
            is LibraryUiEvent.ClearActionMode -> clearActionMode()
        }
    }


    /**
     * Mutators
     */

    private fun onTopBarBackPressed() {
        _activeFolder.update { null }
    }

    private fun onFolderClicked(
        folder: LibraryFolder,
        longClick: Boolean = false
    ) {
        if (longClick) {
            _selectedFolderIds.update { it + folder.id }
            return
        }

        // Short Click
        if(!uiState.value.actionModeUiState.isActionMode) {
            _activeFolder.update { folder }
        } else {
            if(_selectedFolderIds.value.contains(folder.id)) {
                _selectedFolderIds.update { it - folder.id }
            } else {
                _selectedFolderIds.update { it + folder.id }
            }
        }
    }

    private fun onFolderSortMenuChanged(show: Boolean) {
        _showFolderSortMenu.update { show }
    }

    private fun onItemClicked(
        item: LibraryItem,
        longClick: Boolean = false
    ) {
        if (longClick) {
            _selectedItemIds.update { it + item.id }
            return
        }

        // Short Click
        if(!uiState.value.actionModeUiState.isActionMode) {
            _itemToEditId.update { item.id }
            _itemEditData.update {
                LibraryItemEditData(
                    name = item.name,
                    colorIndex = item.colorIndex,
                    folderId = item.libraryFolderId
                )
            }
        } else {
            if(_selectedItemIds.value.contains(item.id)) {
                _selectedItemIds.update { it - item.id }
            } else {
                _selectedItemIds.update { it + item.id }
            }
        }
    }

    private fun onItemSortMenuChanged(show: Boolean) {
        _showItemSortMenu.update { show }
    }

    private fun onDeleteAction() {
        viewModelScope.launch {
            _foldersCache = _selectedFolderIds.value.toList()
            _itemsCache = _selectedItemIds.value.toList()

            libraryUseCases.deleteFolders(_foldersCache)
            libraryUseCases.deleteItems(_itemsCache)

            clearActionMode()
        }
    }

    private fun onRestoreAction() {
        viewModelScope.launch {
            libraryUseCases.restoreFolders(_foldersCache)
            libraryUseCases.restoreItems(_itemsCache)
        }
    }
    private fun onEditAction() {
        _selectedFolderIds.value.firstOrNull()?.let { selectedFolderId ->
            _folderToEditId.update { selectedFolderId }
            _folderEditData.update {
                val folderToEdit = foldersWithItems.value.single {
                    it.folder.id == selectedFolderId
                }.folder
                LibraryFolderEditData(
                    name = folderToEdit.name
                )
            }
        } ?: _selectedItemIds.value.firstOrNull()?.let { selectedItemId ->
            _itemToEditId.update { selectedItemId }
            _itemEditData.update {
                val itemToEdit = items.value.single { it.id == selectedItemId }
                LibraryItemEditData(
                    name = itemToEdit.name,
                    colorIndex = itemToEdit.colorIndex,
                    folderId = itemToEdit.libraryFolderId
                )
            }
        }
        clearActionMode()
    }

    private fun showFolderDialog() {
        _folderEditData.update {
            LibraryFolderEditData(
                name = ""
            )
        }
    }

    private fun showItemDialog() {
        _itemEditData.update {
            LibraryItemEditData(
                name = "",
                colorIndex = (Math.random() * 10).toInt(),
                folderId = _activeFolder.value?.id
            )
        }
    }

    private fun onFolderDialogNameChanged(newName: String) {
        _folderEditData.update { it?.copy(name = newName) }
    }

    private fun onItemDialogNameChanged(newName: String) {
        _itemEditData.update { it?.copy(name = newName) }
    }

    private fun onItemDialogColorIndexChanged(newColorIndex: Int) {
        _itemEditData.update { it?.copy(colorIndex = newColorIndex) }
    }

    private fun onItemDialogFolderIdChanged(newFolderId: UUID?) {
        _itemEditData.update { it?.copy(folderId = newFolderId) }
    }

    private fun clearFolderDialog() {
        _folderToEditId.update { null }
        _folderEditData.update { null }
    }

    private fun clearItemDialog() {
        _itemToEditId.update { null }
        _itemEditData.update { null }
    }

    private fun onFolderDialogConfirmed() {
        viewModelScope.launch {
            val folderData = _folderEditData.value ?: return@launch
            _folderToEditId.value?.let {
                libraryUseCases.editFolder(
                    id = it,
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

    private fun onItemDialogConfirmed() {
        viewModelScope.launch {
            val itemData = _itemEditData.value ?: return@launch
            _itemToEditId.value?.let {
                libraryUseCases.editItem(
                    id = it,
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

    private fun clearActionMode() {
        _selectedItemIds.update { emptySet() }
        _selectedFolderIds.update { emptySet() }
    }

    private fun onItemSortModeSelected(selection: LibraryItemSortMode) {
        _showItemSortMenu.update { false }
        viewModelScope.launch {
            userPreferencesUseCases.selectItemSortMode(selection)
        }
    }

    private fun onFolderSortModeSelected(selection: LibraryFolderSortMode) {
        _showFolderSortMenu.update { false }
        viewModelScope.launch {
            userPreferencesUseCases.selectFolderSortMode(selection)
        }
    }
}