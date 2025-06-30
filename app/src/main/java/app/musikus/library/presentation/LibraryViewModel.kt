/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.lifecycle.viewModelScope
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.library.data.LibraryFolderSortMode
import app.musikus.library.domain.usecase.LibraryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LibraryUiState(
    val actionModeUiState: LibraryActionModeUiState,
    val contentUiState: LibraryContentUiState,
    val dialogsUiState: LibraryDialogsUiState,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryUseCases: LibraryUseCases,
) : LibraryCoreViewModel(
    libraryUseCases = libraryUseCases,
) {

    /**
     *  Imported flows
     */
    private val folderSortInfo = libraryUseCases.getFolderSortInfo().stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = SortInfo(
            mode = LibraryFolderSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    /**
     * Composing the Ui state
     */
    private val foldersSortMenuUiState = folderSortInfo.map{ sortInfo ->
        LibraryFoldersSortMenuUiState(
            mode = sortInfo.mode,
            direction = sortInfo.direction,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryFoldersSortMenuUiState(
            mode = LibraryFolderSortMode.DEFAULT,
            direction = SortDirection.DEFAULT,
        )
    )

    private val foldersUiState = combine(
        foldersWithItems,
        selectedFolderIds,
        foldersSortMenuUiState,
    ) { foldersWithItems, selectedFolders, sortMenuUiState ->
        if (foldersWithItems.isEmpty()) return@combine null

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

    private val contentUiState = combine(
        foldersUiState,
        itemsUiState,
    ) { foldersUiState, itemsUiState ->
        LibraryContentUiState(
            foldersUiState = foldersUiState,
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

    val uiState = combine(
        actionModeUiState,
        contentUiState,
        dialogUiState,
    ) { actionModeUiState, contentUiState, dialogUiState ->
        LibraryUiState(
            actionModeUiState = actionModeUiState,
            contentUiState = contentUiState,
            dialogsUiState = dialogUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryUiState(
            actionModeUiState = actionModeUiState.value,
            contentUiState = contentUiState.value,
            dialogsUiState = dialogUiState.value,
        )
    )

    /**
     * Ui event handler
     */
    fun onUiEvent(event: LibraryUiEvent): Boolean {
        when (event) {
            is LibraryUiEvent.CoreUiEvent -> super.onUiEvent(event.coreEvent)
            is LibraryUiEvent.FolderSortModeSelected -> onFolderSortModeSelected(event.mode)
            is LibraryUiEvent.FolderPressed -> return onFolderClicked(event.folderId, event.longClick)
            is LibraryUiEvent.AddFolderButtonPressed -> showFolderDialog()
        }

        // events are consumed by default
        return true
    }

    /**
     * Mutators
     */
    private fun onFolderClicked(
        folderId: UUID?,
        longClick: Boolean = false
    ): Boolean {
        if (longClick) {
            selectedFolderIds.update { it + folderId }
            return true
        }

        // Short Click
        if (!uiState.value.actionModeUiState.isActionMode) {
            // We return false to indicate that the event was not consumed
            // which should trigger the navigation to the folder details screen
            return false
        } else {
            if (selectedFolderIds.value.contains(folderId)) {
                selectedFolderIds.update { it - folderId }
            } else {
                selectedFolderIds.update { it + folderId }
            }
        }

        return true
    }


    private fun onFolderSortModeSelected(selection: LibraryFolderSortMode) {
        viewModelScope.launch {
            libraryUseCases.selectFolderSortMode(selection)
        }
    }
}
