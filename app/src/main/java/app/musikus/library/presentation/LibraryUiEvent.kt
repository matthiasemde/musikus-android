/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation

import app.musikus.library.data.LibraryFolderSortMode
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.daos.LibraryItem

typealias LibraryUiEventHandler = (LibraryUiEvent) -> Unit

sealed class LibraryUiEvent {

    data object BackButtonPressed : LibraryUiEvent()
    data class FolderPressed(val folder: LibraryFolder, val longClick: Boolean) : LibraryUiEvent()
    data object FolderSortMenuPressed : LibraryUiEvent()
    data class FolderSortModeSelected(val mode: LibraryFolderSortMode) : LibraryUiEvent()

    data class ItemPressed(val item: LibraryItem, val longClick: Boolean) : LibraryUiEvent()
    data object ItemSortMenuPressed : LibraryUiEvent()
    data class ItemSortModeSelected(val mode: LibraryItemSortMode) : LibraryUiEvent()

    data object DeleteButtonPressed : LibraryUiEvent()
    data object DeleteDialogDismissed : LibraryUiEvent()
    data object DeleteDialogConfirmed : LibraryUiEvent()

    data object RestoreButtonPressed : LibraryUiEvent()
    data object EditButtonPressed : LibraryUiEvent()

    data object AddFolderButtonPressed : LibraryUiEvent()
    data object AddItemButtonPressed : LibraryUiEvent()

    data class FolderDialogNameChanged(val name: String) : LibraryUiEvent()
    data object FolderDialogConfirmed : LibraryUiEvent()
    data object FolderDialogDismissed : LibraryUiEvent()

    data object ClearActionMode : LibraryUiEvent()

    data class ItemDialogUiEvent(val dialogEvent: LibraryItemDialogUiEvent) : LibraryUiEvent()
}
