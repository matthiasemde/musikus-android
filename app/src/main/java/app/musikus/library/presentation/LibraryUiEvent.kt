package app.musikus.library.presentation

import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.LibraryItemSortMode

typealias LibraryUiEventHandler = (LibraryUiEvent) -> Unit

sealed class LibraryUiEvent  {

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