/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation

import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryItem

typealias LibraryCoreUiEventHandler = (LibraryCoreUiEvent) -> Boolean

sealed class LibraryCoreUiEvent {
    data class ItemPressed(val item: LibraryItem, val longClick: Boolean) : LibraryCoreUiEvent()
    data object ItemSortMenuPressed : LibraryCoreUiEvent()
    data class ItemSortModeSelected(val mode: LibraryItemSortMode) : LibraryCoreUiEvent()

    data object DeleteButtonPressed : LibraryCoreUiEvent()
    data object DeleteDialogDismissed : LibraryCoreUiEvent()
    data object DeleteDialogConfirmed : LibraryCoreUiEvent()
    data object RestoreButtonPressed : LibraryCoreUiEvent()

    data object EditButtonPressed : LibraryCoreUiEvent()

    data object AddItemButtonPressed : LibraryCoreUiEvent()

    data object ClearActionMode : LibraryCoreUiEvent()

    data class ItemDialogUiEvent(val dialogEvent: LibraryItemDialogUiEvent) : LibraryCoreUiEvent()
    data class FolderDialogUiEvent(val dialogEvent: LibraryFolderDialogUiEvent) : LibraryCoreUiEvent()
}
