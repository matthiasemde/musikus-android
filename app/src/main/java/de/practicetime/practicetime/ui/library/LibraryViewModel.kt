/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.ui.library

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.shared.MultiFABState
import de.practicetime.practicetime.shared.SpinnerState
import java.util.*

enum class LibraryMenuSelections {
}

enum class LibraryItemSortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    NAME,
    COLOR,
    CUSTOM;

    companion object {
        fun toString(sortMode: LibraryItemSortMode) = when (sortMode) {
            DATE_ADDED -> "Date added"
            LAST_MODIFIED -> "Last modified"
            NAME -> "Name"
            COLOR -> "Color"
            CUSTOM -> "Custom"
        }
    }
}

enum class LibraryFolderSortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    CUSTOM;

    companion object {
        fun toString(sortMode: LibraryFolderSortMode) = when (sortMode) {
            DATE_ADDED -> "Date added"
            LAST_MODIFIED -> "Last modified"
            CUSTOM -> "Custom"
        }
    }
}

enum class DialogMode {
    ADD,
    EDIT
}

class LibraryViewModel(

) : ViewModel() {

    init {
        Log.d("LibraryViewModel", "initialized")
    }

    // Menu
    var showFolderSortModeMenu = mutableStateOf(false)
    var showItemSortModeMenu = mutableStateOf(false)

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
    var itemDialogFolderId = mutableStateOf<UUID?>(null)
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

    val selectedItemIds = mutableStateListOf<UUID>()
    val selectedFolderIds = mutableStateListOf<UUID>()

    fun clearActionMode() {
        selectedItemIds.clear()
        selectedFolderIds.clear()
        actionMode.value = false
    }
}
