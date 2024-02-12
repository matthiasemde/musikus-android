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
import app.musikus.ui.components.TopBarUiState
import app.musikus.utils.SortDirection
import app.musikus.utils.SortMode
import java.util.UUID

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
    val selectedFolderIds: Set<UUID>,

    val sortMenuUiState: LibraryFoldersSortMenuUiState
)

data class LibraryItemsSortMenuUiState(
    val show: Boolean,

    val mode: SortMode<LibraryItem>,
    val direction: SortDirection,
)

data class LibraryItemsUiState(
    val items: List<LibraryItem>,
    val selectedItemIds: Set<UUID>,

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
    val folderToEditId: UUID?,
)

data class LibraryLibraryItemDialogUiState(
    override val mode: DialogMode,
    override val itemData: LibraryItemEditData,
    override val folders : List<LibraryFolder>,
    override val isConfirmButtonEnabled: Boolean,
) : LibraryItemDialogUiState

data class LibraryDeleteDialogUiState(
    val numberOfSelectedFolders: Int,
    val numberOfSelectedItems: Int,
)

data class LibraryDialogUiState(
    val folderDialogUiState: LibraryFolderDialogUiState?,
    val itemDialogUiState: LibraryItemDialogUiState?,
    val deleteDialogUiState: LibraryDeleteDialogUiState?,
)

data class LibraryFabUiState(
    val activeFolder: LibraryFolder?,
)

data class LibraryUiState (
    val topBarUiState: LibraryTopBarUiState,
    val actionModeUiState: LibraryActionModeUiState,
    val contentUiState: LibraryContentUiState,
    val dialogUiState: LibraryDialogUiState,
    val fabUiState: LibraryFabUiState,
)