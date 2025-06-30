/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.library.presentation

import app.musikus.core.data.LibraryFolderWithItems
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortMode
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem
import java.time.ZonedDateTime
import java.util.UUID

data class LibraryActionModeUiState(
    val isActionMode: Boolean,
    val numberOfSelections: Int,
)

data class LibraryFoldersSortMenuUiState(
    val mode: SortMode<LibraryFolder>,
    val direction: SortDirection,
)

data class LibraryFoldersUiState(
    val foldersWithItems: List<LibraryFolderWithItems>,
    val selectedFolderIds: Set<UUID?>,

    val sortMenuUiState: LibraryFoldersSortMenuUiState
)

data class LibraryItemsSortMenuUiState(
    val mode: SortMode<LibraryItem>,
    val direction: SortDirection,
)

data class LibraryItemsUiState(
    val itemsWithLastPracticedDate: List<Pair<LibraryItem, ZonedDateTime?>>,
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
    override val folders: List<LibraryFolder>,
    override val isConfirmButtonEnabled: Boolean,
) : LibraryItemDialogUiState

data class LibraryDeleteDialogUiState(
    val numberOfSelectedFolders: Int,
    val numberOfSelectedItems: Int,
)

data class LibraryDialogsUiState(
    val folderDialogUiState: LibraryFolderDialogUiState?,
    val itemDialogUiState: LibraryItemDialogUiState?,
    val deleteDialogUiState: LibraryDeleteDialogUiState?,
)
