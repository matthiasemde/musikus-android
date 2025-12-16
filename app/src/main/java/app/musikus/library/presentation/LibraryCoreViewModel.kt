/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.core.data.Nullable
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryFolderUpdateAttributes
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.library.data.entities.LibraryItemUpdateAttributes
import app.musikus.library.domain.usecase.LibraryUseCases
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
abstract class LibraryCoreViewModel(
    private val libraryUseCases: LibraryUseCases,
) : ViewModel() {

    /** Private variables */
    private var _foldersCache = emptyList<UUID>()
    private var _itemsCache = emptyList<UUID>()

    /** Imported flows */
    protected val foldersWithItems = libraryUseCases.getSortedFolders().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val itemSortInfo = libraryUseCases.getItemSortInfo().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortInfo(
            mode = LibraryItemSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    /** Own state flows */
    // Active folder
    protected val activeFolderId = MutableStateFlow<UUID?>(null)

    // Folder dialog
    private val _folderEditData = MutableStateFlow<LibraryFolderEditData?>(null)
    private val _folderToEditId = MutableStateFlow<UUID?>(null)

    // Item dialog
    private val _itemEditData = MutableStateFlow<LibraryItemEditData?>(null)
    private val _itemToEditId = MutableStateFlow<UUID?>(null)

    // Action mode
    protected val selectedFolderIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val _selectedItemIds = MutableStateFlow<Set<UUID>>(emptySet())

    // Delete dialog
    private val _showDeleteDialog = MutableStateFlow(false)

    /** Combining imported and own flows  */
    private val items = activeFolderId.flatMapLatest { activeFolderId ->
        libraryUseCases.getSortedItems(Nullable(activeFolderId))
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
    protected val actionModeUiState = combine(
        selectedFolderIds,
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

    protected val itemsSortMenuUiState = itemSortInfo.map { sortInfo ->
        LibraryItemsSortMenuUiState(
            mode = sortInfo.mode,
            direction = sortInfo.direction,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryItemsSortMenuUiState(
            mode = LibraryItemSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    protected val itemsUiState = combine(
        items,
        lastPracticedDates,
        _selectedItemIds,
        itemsSortMenuUiState,
    ) { items, lastPracticedDates, selectedItems, sortMenuUiState ->
        if (items.isEmpty()) return@combine null

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

    private val folderDialogUiState = combine(
        _folderEditData,
        _folderToEditId,
    ) { editData, folderToEditId ->
        if (editData == null) return@combine null
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
        if (editData == null) return@combine null
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
        selectedFolderIds,
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

    protected val dialogUiState = combine(
        folderDialogUiState,
        itemDialogUiState,
        deleteDialogUiState
    ) { folderDialogUiState, itemDialogUiState, deleteDialogUiState ->
        assert(folderDialogUiState == null || itemDialogUiState == null)
        LibraryDialogsUiState(
            folderDialogUiState = folderDialogUiState,
            itemDialogUiState = itemDialogUiState,
            deleteDialogUiState = deleteDialogUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryDialogsUiState(
            folderDialogUiState = folderDialogUiState.value,
            itemDialogUiState = itemDialogUiState.value,
            deleteDialogUiState = deleteDialogUiState.value,
        )
    )

    /**
     * Ui event handler
     */
    protected fun onUiEvent(event: LibraryCoreUiEvent): Boolean {
        when (event) {
            is LibraryCoreUiEvent.ItemPressed -> onItemClicked(event.item, event.longClick)
            is LibraryCoreUiEvent.ItemSortModeSelected -> onItemSortModeSelected(event.mode)
            is LibraryCoreUiEvent.DeleteButtonPressed -> _showDeleteDialog.update { true }
            is LibraryCoreUiEvent.DeleteDialogDismissed -> {
                _showDeleteDialog.update { false }
                clearActionMode()
            }
            is LibraryCoreUiEvent.DeleteDialogConfirmed -> {
                _showDeleteDialog.update { false }
                onDeleteAction()
            }
            is LibraryCoreUiEvent.RestoreButtonPressed -> onRestoreAction()
            is LibraryCoreUiEvent.EditButtonPressed -> onEditAction()
            is LibraryCoreUiEvent.AddItemButtonPressed -> showItemDialog()
            is LibraryCoreUiEvent.FolderDialogUiEvent -> onFolderDialogUiEvent(event.dialogEvent)
            is LibraryCoreUiEvent.ItemDialogUiEvent -> onItemDialogUiEvent(event.dialogEvent)
            is LibraryCoreUiEvent.ClearActionMode -> clearActionMode()
        }
        // all events are handled, so we always return true
        return true
    }

    /**
     * Mutators
     */
    private fun onFolderDialogUiEvent(event: LibraryFolderDialogUiEvent) {
        when (event) {
            is LibraryFolderDialogUiEvent.NameChanged -> onFolderDialogNameChanged(event.name)
            is LibraryFolderDialogUiEvent.Confirmed -> onFolderDialogConfirmed()
            is LibraryFolderDialogUiEvent.Dismissed -> clearFolderDialog()
        }
    }

    private fun onItemDialogUiEvent(event: LibraryItemDialogUiEvent) {
        when (event) {
            is LibraryItemDialogUiEvent.NameChanged -> onItemDialogNameChanged(event.name)
            is LibraryItemDialogUiEvent.ColorIndexChanged -> onItemDialogColorIndexChanged(
                event.colorIndex
            )
            is LibraryItemDialogUiEvent.FolderIdChanged -> onItemDialogFolderIdChanged(event.folderId)
            is LibraryItemDialogUiEvent.Confirmed -> onItemDialogConfirmed()
            is LibraryItemDialogUiEvent.Dismissed -> clearItemDialog()
        }
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
        if (!actionModeUiState.value.isActionMode) {
            _itemToEditId.update { item.id }
            _itemEditData.update {
                LibraryItemEditData(
                    name = item.name,
                    colorIndex = item.colorIndex,
                    folderId = item.libraryFolderId
                )
            }
        } else {
            if (_selectedItemIds.value.contains(item.id)) {
                _selectedItemIds.update { it - item.id }
            } else {
                _selectedItemIds.update { it + item.id }
            }
        }
    }

    private fun onDeleteAction() {
        viewModelScope.launch {
            // only delete actual folders (not root=null)
            _foldersCache = selectedFolderIds.value.toList()
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
        _selectedItemIds.value.firstOrNull()?.let { selectedItemId ->
            _itemToEditId.update { selectedItemId }
            _itemEditData.update {
                val itemToEdit = items.value.single { it.id == selectedItemId }
                LibraryItemEditData(
                    name = itemToEdit.name,
                    colorIndex = itemToEdit.colorIndex,
                    folderId = itemToEdit.libraryFolderId
                )
            }
        } ?: (selectedFolderIds.value.firstOrNull() ?: activeFolderId.value)?.let { selectedFolderId ->
            _folderToEditId.update { selectedFolderId }
            _folderEditData.update {
                val folderToEdit = foldersWithItems.value.single {
                    it.folder.id == selectedFolderId
                }.folder
                LibraryFolderEditData(
                    name = folderToEdit.name
                )
            }
        }
        clearActionMode()
    }

    protected fun showFolderDialog() {
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
                folderId = activeFolderId.value
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
                    LibraryItemUpdateAttributes(
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
        selectedFolderIds.update { emptySet() }
    }

    private fun onItemSortModeSelected(selection: LibraryItemSortMode) {
        viewModelScope.launch {
            libraryUseCases.selectItemSortMode(selection)
        }
    }
}
