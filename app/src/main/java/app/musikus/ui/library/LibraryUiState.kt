/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.library

import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.shared.TopBarUiState
import app.musikus.utils.SortDirection
import app.musikus.utils.SortMode

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

    val mode: SortMode<LibraryFolder>,
    val direction: SortDirection,
)

data class LibraryFoldersUiState(
    val foldersWithItems: List<LibraryFolderWithItems>,
    val selectedFolders: Set<LibraryFolder>,

    val sortMenuUiState: LibraryFoldersSortMenuUiState
)

data class LibraryItemsSortMenuUiState(
    val show: Boolean,

    val mode: SortMode<LibraryItem>,
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