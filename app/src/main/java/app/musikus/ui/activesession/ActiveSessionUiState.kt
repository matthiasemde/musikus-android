/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.activesession

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.LibraryItem
import java.util.UUID


// TODO UIText for all Strings?

enum class SessionPausedResumedState {
    RUNNING,
    PAUSED
}

/** Main UI State */
data class ActiveSessionUiState(
    val topBarUiState: ActiveSessionTopBarUiState = ActiveSessionTopBarUiState(),
    val mainContentUiState: MainContentUiState = MainContentUiState(),
    val newItemSelectorUiState: NewItemSelectorUiState = NewItemSelectorUiState(),
    val toolsUiState: ActiveSessionToolsUiState = ActiveSessionToolsUiState(),
)

@Stable
data class MainContentUiState(
    val timerUiState: ActiveSessionTimerUiState = ActiveSessionTimerUiState(),
    val currentItemUiState: ActiveSessionCurrentItemUiState = ActiveSessionCurrentItemUiState(),
    val pastSectionsUiState: ActiveSessionCompletedSectionsUiState = ActiveSessionCompletedSectionsUiState(),
)

data class ActiveSessionTopBarUiState(
    val visible: Boolean = false,
    val pauseButtonAppearance: SessionPausedResumedState = SessionPausedResumedState.RUNNING,
)

data class ActiveSessionTimerUiState(
    val timerText: String = "",
    val subHeadingAppearance: SessionPausedResumedState = SessionPausedResumedState.RUNNING,
    val subHeadingText: String = ""
)

data class ActiveSessionCurrentItemUiState(
    val visible: Boolean = false,
    val name: String = "",
    val color: Color = Color.Transparent,
    val durationText: String = "",
)

data class ActiveSessionCompletedSectionsUiState(
    val visible: Boolean = false,
    val items: List<CompletedSectionUiState> = emptyList()
)

data class CompletedSectionUiState(
    val id: UUID = UUIDConverter.deadBeef,
    val name: String = "",
    val durationText: String = "",
    val color: Color = Color.Transparent,
)

data class NewItemSelectorUiState(
    val visible: Boolean = false,
    val selectedFolderId: UUID? = null,
    val runningItemFolderId: UUID? = null,
    val foldersWithItems: List<LibraryFolderWithItems> = emptyList()
)

data class ActiveSessionToolsUiState(
    val activeTab: ActiveSessionTab = ActiveSessionTab.entries[0],
    val expanded: Boolean = false,
//    val metronomeState: MetronomeUiState,
//    val recorderState: RecorderUiState
)

enum class ActiveSessionTab {
    METRONOME, RECORDER
}

sealed class ActiveSessionUiEvent {
    data object ToggleNewItemSelectorVisible : ActiveSessionUiEvent()
    data object TogglePauseState : ActiveSessionUiEvent()
    data object ShowMetronome : ActiveSessionUiEvent()
    data object ShowRecorder : ActiveSessionUiEvent()
    data class SelectFolder(val folderId: UUID?) : ActiveSessionUiEvent()
    data class SelectItem(val item: LibraryItem) : ActiveSessionUiEvent()
    data object ShowFinishDialog : ActiveSessionUiEvent()
    data object BackPressed : ActiveSessionUiEvent()
    data object ShowDiscardSessionDialog : ActiveSessionUiEvent()
    data class DeleteSection(val sectionId: UUID) : ActiveSessionUiEvent()
}

// -------------------------------------------------------------------------------------------------
/*

data class ActiveSessionUiStateOld(
    val cardUiStates: List<ActiveSessionDraggableCardUiState>,
    val totalSessionDuration: Duration,
    val ongoingPauseDuration: Duration,
    val sections: List<ActiveSessionSectionListItemUiState>,
    val runningSection: ActiveSessionSectionListItemUiState?,
    val isPaused: Boolean,
    val addItemDialogUiState: ActiveSessionAddLibraryItemDialogUiState?,
    val dialogUiState: ActiveSessionDialogUiState,
    val libraryCardUiState: ActiveSessionDraggableCardUiState.LibraryCardUiState
)

data class ActiveSessionDialogUiState(
    val showDiscardSessionDialog: Boolean,
    val endDialogUiState: ActiveSessionEndDialogUiState?,
)

data class ActiveSessionEndDialogUiState(
    val rating: Int,
    val comment: String,
)

data class ActiveSessionAddLibraryItemDialogUiState(
    override val folders: List<LibraryFolder>,
    override val itemData: LibraryItemEditData,
    override val isConfirmButtonEnabled: Boolean,
    override val mode: DialogMode = DialogMode.ADD,
) : LibraryItemDialogUiState

data class ActiveSessionSectionListItemUiState(
    val id: UUID,
    val color: Color,
    val libraryItem: LibraryItem,
    val duration: Duration
)

sealed class ActiveSessionDraggableCardHeaderUiState : DraggableCardHeaderUiState {
    data class LibraryCardHeaderUiState(
        val folders: List<LibraryFolder?>,
        val selectedFolderId: UUID?,
        val activeFolderId: Nullable<UUID>?, // null = no active folder, Nullable(null) = root folder
    ) : ActiveSessionDraggableCardHeaderUiState()

    data object RecorderCardHeaderUiState : ActiveSessionDraggableCardHeaderUiState()
    data object MetronomeCardHeaderUiState : ActiveSessionDraggableCardHeaderUiState()
}

sealed class ActiveSessionDraggableCardBodyUiState : DraggableCardBodyUiState {
    data class LibraryCardBodyUiState(
        val itemsWithLastPracticedDate: List<Pair<LibraryItem, ZonedDateTime?>>,
        val activeItemId: UUID?
    ) : ActiveSessionDraggableCardBodyUiState()

    data object RecorderCardBodyUiState : ActiveSessionDraggableCardBodyUiState()
    data object MetronomeCardBodyUiState : ActiveSessionDraggableCardBodyUiState()
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
    ) : ActiveSessionDraggableCardUiState()

    data class RecorderCardUiState(
        override val headerUiState: ActiveSessionDraggableCardHeaderUiState.RecorderCardHeaderUiState,
        override val bodyUiState: ActiveSessionDraggableCardBodyUiState.RecorderCardBodyUiState,
        override val title: String,
        override val isExpandable: Boolean,
        override val hasFab: Boolean,
    ) : ActiveSessionDraggableCardUiState()

    data class MetronomeCardUiState(
        override val headerUiState: ActiveSessionDraggableCardHeaderUiState.MetronomeCardHeaderUiState,
        override val bodyUiState: ActiveSessionDraggableCardBodyUiState.MetronomeCardBodyUiState,
        override val title: String,
        override val isExpandable: Boolean,
        override val hasFab: Boolean,
    ) : ActiveSessionDraggableCardUiState()
}

sealed class ActiveSessionUiEvent : DraggableCardUiEvent {

    data class SelectFolder(val folderId: UUID?) : ActiveSessionUiEvent()
    data class SelectItem(val item: LibraryItem) : ActiveSessionUiEvent()
    data class DeleteSection(val sectionId: UUID) : ActiveSessionUiEvent()
    data object BackPressed : ActiveSessionUiEvent()
    data object ShowDiscardSessionDialog : ActiveSessionUiEvent()
    data object DiscardSessionDialogConfirmed : ActiveSessionUiEvent()
    data object DiscardSessionDialogDismissed : ActiveSessionUiEvent()
    data object TogglePause : ActiveSessionUiEvent()
    data object ShowFinishDialog : ActiveSessionUiEvent()
    data object CreateNewLibraryItem : ActiveSessionUiEvent()
    data class ItemDialogUiEvent(val dialogEvent: LibraryItemDialogUiEvent) : ActiveSessionUiEvent()
    data class EndDialogRatingChanged(val rating: Int) : ActiveSessionUiEvent()
    data class EndDialogCommentChanged(val comment: String) : ActiveSessionUiEvent()
    data object EndDialogDismissed : ActiveSessionUiEvent()
    data object EndDialogConfirmed : ActiveSessionUiEvent()
}

typealias ActiveSessionUiEventHandler = (ActiveSessionUiEvent) -> Unit

*/