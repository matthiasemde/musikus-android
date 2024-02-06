/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.activesession

import app.musikus.database.Nullable
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.library.LibraryItemEditData
import java.util.UUID
import kotlin.time.Duration



data class ActiveSessionUiState(
    val libraryCardUiState: ActiveSessionDraggableCardUiState.LibraryCardUiState,
    val totalSessionDuration: Duration,
    val totalBreakDuration: Duration,
    val sections: List<SectionListItemUiState>,
    val isPaused: Boolean,
    val newLibraryItemData: LibraryItemEditData?
)

data class SectionListItemUiState(
    val id: Int,
    val libraryItem: LibraryItem,
    val duration: Duration
)

sealed class ActiveSessionDraggableCardHeaderUiState : DraggableCardHeaderUiState {
    data class LibraryCardHeaderUiState(
        val folders: List<LibraryFolder?>,
        val selectedFolderId: UUID?,
        val activeFolderId: Nullable<UUID>?, // null = no active folder, Nullable(null) = root folder
    ) : ActiveSessionDraggableCardHeaderUiState()
}

sealed class ActiveSessionDraggableCardBodyUiState : DraggableCardBodyUiState {
    data class LibraryCardBodyUiState(
        val items: List<LibraryItem>,
        val activeItemId: UUID?
    ) : ActiveSessionDraggableCardBodyUiState()
}

sealed class ActiveSessionDraggableCardUiState : DraggableCardUiState<
        ActiveSessionDraggableCardHeaderUiState,
        ActiveSessionDraggableCardBodyUiState
> {
    data class LibraryCardUiState(
        override val headerUiState: ActiveSessionDraggableCardHeaderUiState.LibraryCardHeaderUiState,
        override val bodyUiState: ActiveSessionDraggableCardBodyUiState.LibraryCardBodyUiState,
        override val title: String,
        override val isExpandable: Boolean,
        override val hasFab: Boolean,
        override val fabAction: () -> Unit,
    ) : ActiveSessionDraggableCardUiState()
}

sealed class ActiveSessionUiEvent : DraggableCardUiEvent() {
    data class SelectFolder(val folderId: UUID?) : ActiveSessionUiEvent()
    data class SelectItem(val item: LibraryItem) : ActiveSessionUiEvent()

    data class DeleteSection(val sectionId: Int) : ActiveSessionUiEvent()

    data object TogglePause : ActiveSessionUiEvent()
    data object StopSession : ActiveSessionUiEvent()

}

typealias ActiveSessionUiEventHandler = (ActiveSessionUiEvent) -> Unit