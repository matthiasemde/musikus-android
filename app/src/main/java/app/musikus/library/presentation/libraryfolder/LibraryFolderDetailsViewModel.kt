/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation.libraryfolder

import androidx.lifecycle.viewModelScope
import app.musikus.R
import app.musikus.core.presentation.utils.UiText
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.library.presentation.LibraryActionModeUiState
import app.musikus.library.presentation.LibraryCoreUiEvent
import app.musikus.library.presentation.LibraryCoreViewModel
import app.musikus.library.presentation.LibraryDialogsUiState
import app.musikus.library.presentation.LibraryItemsSortMenuUiState
import app.musikus.library.presentation.LibraryItemsUiState
import app.musikus.settings.domain.usecase.UserPreferencesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject


typealias LibraryFolderDetailsUiEventHandler = (LibraryFolderDetailsUiEvent) -> Boolean

sealed class LibraryFolderDetailsUiEvent {
    data class CoreUiEvent(val coreEvent: LibraryCoreUiEvent) : LibraryFolderDetailsUiEvent()
}

data class LibraryFolderDetailsUiState(
    val folderName: UiText,
    val itemsSortMenuUiState: LibraryItemsSortMenuUiState,
    val actionModeUiState: LibraryActionModeUiState,
    val itemsUiState: LibraryItemsUiState?,
    val dialogsUiState: LibraryDialogsUiState,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryFolderDetailsViewModel @Inject constructor(
    libraryUseCases: LibraryUseCases,
    userPreferencesUseCases: UserPreferencesUseCases,
) : LibraryCoreViewModel(
    libraryUseCases,
    userPreferencesUseCases,
) {

    /**
     * Combined flows
     */
    private val activeFolder = combine(
        activeFolderId,
        foldersWithItems,
    ) { activeFolderId, foldersWithItems ->
        foldersWithItems.find { it.folder.id == activeFolderId }
    }

    /**
     * Composing the Ui state
     */
    val uiState = combine(
        activeFolder,
        itemsSortMenuUiState,
        actionModeUiState,
        itemsUiState,
        dialogUiState,
    ) { activeFolder, itemsSortMenuUiState, actionModeUiState, itemsUiState, dialogUiState ->
        LibraryFolderDetailsUiState(
            folderName = activeFolder?.folder?.name?.let {
                    UiText.DynamicString(it)
                } ?: UiText.StringResource(R.string.library_folder_details_folder_not_found),
            itemsSortMenuUiState = itemsSortMenuUiState,
            actionModeUiState = actionModeUiState,
            itemsUiState = itemsUiState,
            dialogsUiState = dialogUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = LibraryFolderDetailsUiState(
            folderName = UiText.DynamicString(""),
            itemsSortMenuUiState = itemsSortMenuUiState.value,
            actionModeUiState = actionModeUiState.value,
            itemsUiState = itemsUiState.value,
            dialogsUiState = dialogUiState.value,
        )
    )

    /**
     * Ui event handler
     */
    fun onUiEvent(event: LibraryFolderDetailsUiEvent) : Boolean {
        when (event) {
            is LibraryFolderDetailsUiEvent.CoreUiEvent -> {
                super.onUiEvent(event.coreEvent)
            }
        }

        // events are consumed by default
        return true
    }

    /**
     * Mutators
     */
    fun setActiveFolder(folderId: UUID) {
        activeFolderId.update { folderId }
    }
}
