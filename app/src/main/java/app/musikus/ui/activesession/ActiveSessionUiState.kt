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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZonedDateTime
import java.util.UUID


// TODO UIText for all Strings?

@Stable
enum class SessionPausedResumedState {
    RUNNING,
    PAUSED
}

/** Specific one-time events triggering some UI action in ActiveSession */
@Stable
data class ActiveSessionEventStates (
    val sessionSaved: Boolean = false,
    val sessionDiscarded: Boolean = false,
)

@Stable
/** Main UI State */
data class ActiveSessionUiState(
    val topBarUiState: StateFlow<ActiveSessionTopBarUiState> = MutableStateFlow(ActiveSessionTopBarUiState()),
    val mainContentUiState: StateFlow<MainContentUiState> = MutableStateFlow(MainContentUiState()),
    val newItemSelectorUiState: StateFlow<NewItemSelectorUiState> = MutableStateFlow(NewItemSelectorUiState()),
    val toolsUiState: StateFlow<ActiveSessionToolsUiState> = MutableStateFlow(ActiveSessionToolsUiState()),
    val dialogVisibilities: StateFlow<DialogVisibilities> = MutableStateFlow(DialogVisibilities()),
)

data class DialogVisibilities(
    val newItemSelectorVisible: Boolean = false,
    val finishDialogVisible: Boolean = false,
    val discardDialogVisible: Boolean = false,
    val createItemDialogVisible: Boolean = false,
    val createFolderDialogVisible: Boolean = false,
)

@Stable
data class MainContentUiState(
    val timerUiState: StateFlow<ActiveSessionTimerUiState> = MutableStateFlow(ActiveSessionTimerUiState()),
    val currentItemUiState: StateFlow<ActiveSessionCurrentItemUiState> = MutableStateFlow(ActiveSessionCurrentItemUiState()),
    val pastSectionsUiState: StateFlow<ActiveSessionCompletedSectionsUiState> = MutableStateFlow(ActiveSessionCompletedSectionsUiState()),
    val endDialogUiState: StateFlow<ActiveSessionEndDialogUiState> = MutableStateFlow(ActiveSessionEndDialogUiState())
)

@Stable
data class ActiveSessionTopBarUiState(
    val visible: Boolean = false,
    val pauseButtonAppearance: SessionPausedResumedState = SessionPausedResumedState.RUNNING,
)

@Stable
data class ActiveSessionTimerUiState(
    val timerText: String = "00:00", // Prevents size change animation to trigger when opening the screen
    val subHeadingAppearance: SessionPausedResumedState = SessionPausedResumedState.RUNNING,
    val subHeadingText: String = ""
)

@Stable
data class ActiveSessionCurrentItemUiState(
    val visible: Boolean = false,
    val name: String = "",
    val color: Color = Color.Transparent,
    val durationText: String = "",
)

@Stable
data class ActiveSessionCompletedSectionsUiState(
    val visible: Boolean = false,
    val items: List<CompletedSectionUiState> = emptyList()
)

@Stable
data class ActiveSessionEndDialogUiState(
    val rating: Int = 5,
    val comment: String = "",
)

@Stable
data class CompletedSectionUiState(
    val id: UUID = UUIDConverter.deadBeef,
    val name: String = "",
    val durationText: String = "",
    val color: Color = Color.Transparent,
)

@Stable
data class NewItemSelectorUiState(
    val runningItem: LibraryItem? = null,
    val foldersWithItems: List<LibraryFolderWithItems> = emptyList(),
    val lastPracticedDates: Map<UUID, ZonedDateTime?> = emptyMap(),
    val rootItems: List<LibraryItem> = emptyList()
)

@Stable
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
    data object TogglePauseState : ActiveSessionUiEvent()
    data class SelectItem(val item: LibraryItem) : ActiveSessionUiEvent()
    data object BackPressed : ActiveSessionUiEvent()
    data class DeleteSection(val sectionId: UUID) : ActiveSessionUiEvent()
    data class EndDialogUiEvent(val dialogEvent: ActiveSessionEndDialogUiEvent) : ActiveSessionUiEvent()
    data object DiscardSessionDialogConfirmed : ActiveSessionUiEvent()
    data object ToggleNewItemSelector: ActiveSessionUiEvent()
    data object ToggleFinishDialog : ActiveSessionUiEvent()
    data object ToggleDiscardDialog : ActiveSessionUiEvent()
    data object ToggleCreateItemDialog: ActiveSessionUiEvent()
    data object ToggleCreateFolderDialog: ActiveSessionUiEvent()

}

sealed class ActiveSessionEndDialogUiEvent {
    data class RatingChanged(val rating: Int) : ActiveSessionEndDialogUiEvent()
    data class CommentChanged(val comment: String) : ActiveSessionEndDialogUiEvent()
    data object Confirmed : ActiveSessionEndDialogUiEvent()
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