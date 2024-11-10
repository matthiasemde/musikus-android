/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation

import app.musikus.library.data.LibraryFolderSortMode
import app.musikus.library.data.daos.LibraryFolder

// returns true if the event was consumed, false otherwise
typealias LibraryUiEventHandler = (LibraryUiEvent) -> Boolean

sealed class LibraryUiEvent {
    data class CoreUiEvent(val coreEvent: LibraryCoreUiEvent) : LibraryUiEvent()

    data object FolderSortMenuPressed : LibraryUiEvent()
    data class FolderSortModeSelected(val mode: LibraryFolderSortMode) : LibraryUiEvent()

    data class FolderPressed(val folder: LibraryFolder, val longClick: Boolean) : LibraryUiEvent()

    data object AddFolderButtonPressed : LibraryUiEvent()
}
