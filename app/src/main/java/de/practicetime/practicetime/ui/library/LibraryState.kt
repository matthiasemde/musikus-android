package de.practicetime.practicetime.ui.library

import androidx.compose.runtime.*
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.shared.MultiFABState
import de.practicetime.practicetime.shared.SpinnerState
import kotlinx.coroutines.CoroutineScope

enum class LibraryMenuSelections {
    SORT_BY,
}

enum class LibrarySortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    NAME,
    COLOR,
    CUSTOM;

    companion object {
        fun toString(sortMode: LibrarySortMode): String {
            return when (sortMode) {
                DATE_ADDED -> "Date added"
                LAST_MODIFIED -> "Last modified"
                NAME -> "Name"
                COLOR -> "Color"
                CUSTOM -> "Custom"
            }
        }
    }
}

enum class DialogMode {
    ADD,
    EDIT
}

class LibraryState(
    private val coroutineScope: CoroutineScope,
) {
    // Menu
    var showSortModeMenu = mutableStateOf(false)

    val activeFolder = mutableStateOf<LibraryFolder?>(null)

    // Folder dialog
    var showFolderDialog = mutableStateOf(false)
    var editableFolder = mutableStateOf<LibraryFolder?>(null)
    var folderDialogMode = mutableStateOf(DialogMode.ADD)
    var folderDialogName = mutableStateOf("")

    fun clearFolderDialog() {
        showFolderDialog.value = false
        editableFolder.value = null
        folderDialogName.value = ""
    }

    // Item dialog
    var showItemDialog = mutableStateOf(false)
    var editableItem = mutableStateOf<LibraryItem?>(null)
    var itemDialogMode = mutableStateOf(DialogMode.ADD)
    var itemDialogName = mutableStateOf("")
    var itemDialogColorIndex = mutableStateOf(0)
    var itemDialogFolderId = mutableStateOf<Long?>(null)
    var itemDialogFolderSelectorExpanded = mutableStateOf(SpinnerState.COLLAPSED)

    fun clearItemDialog() {
        showItemDialog.value = false
        editableItem.value = null
        itemDialogName.value = ""
        itemDialogColorIndex.value = 0
        itemDialogFolderId.value = null
        itemDialogFolderSelectorExpanded.value = SpinnerState.COLLAPSED
    }

    // Multi FAB
    var multiFABState = mutableStateOf(MultiFABState.COLLAPSED)

    // Action mode
    var actionMode = mutableStateOf(false)

    val selectedItemIds = mutableStateListOf<Long>()
    val selectedFolderIds = mutableStateListOf<Long>()

    fun clearActionMode() {
        selectedItemIds.clear()
        selectedFolderIds.clear()
        actionMode.value = false
    }
}

@Composable
fun rememberLibraryState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember(coroutineScope) { LibraryState(coroutineScope) }
