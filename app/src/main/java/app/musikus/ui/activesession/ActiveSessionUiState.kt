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


@Stable
enum class ActiveSessionState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    UNKNOWN
}

@Stable
/** Main UI State */
data class ActiveSessionUiState(
    val sessionState: StateFlow<ActiveSessionState> = MutableStateFlow(ActiveSessionState.NOT_STARTED),
    val mainContentUiState: StateFlow<ActiveSessionContentUiState> = MutableStateFlow(ActiveSessionContentUiState()),
    val newItemSelectorUiState: StateFlow<NewItemSelectorUiState> = MutableStateFlow(NewItemSelectorUiState()),
    val toolsUiState: StateFlow<ActiveSessionToolsUiState> = MutableStateFlow(ActiveSessionToolsUiState()),
    val dialogVisibilities: StateFlow<ActiveSessionDialogsUiState> = MutableStateFlow(ActiveSessionDialogsUiState()),
)

data class ActiveSessionDialogsUiState(
    val newItemSelectorVisible: Boolean = false,
    val finishDialogVisible: Boolean = false,
    val discardDialogVisible: Boolean = false,
    val createItemDialogVisible: Boolean = false,
    val createFolderDialogVisible: Boolean = false,
)

@Stable
data class ActiveSessionContentUiState(
    /**
     * Nullable() States indicate visibility of the UI element.
     * Use Nullable because State<Class?> cannot be smart-casted in Composables
     */
    val timerUiState: StateFlow<ActiveSessionTimerUiState> = MutableStateFlow(ActiveSessionTimerUiState()),
    val currentItemUiState: StateFlow<ActiveSessionCurrentItemUiState?> = MutableStateFlow(null),
    val pastSectionsUiState: StateFlow<ActiveSessionCompletedSectionsUiState?> = MutableStateFlow(null),
    val endDialogUiState: StateFlow<ActiveSessionEndDialogUiState> = MutableStateFlow(ActiveSessionEndDialogUiState())
)

@Stable
data class ActiveSessionTimerUiState(
    val timerText: String = "00:00", // Prevents size change animation to trigger when opening the screen
    val subHeadingText: String = ""
)

@Stable
data class ActiveSessionCurrentItemUiState(
    val name: String = "",
    val color: Color = Color.Transparent,
    val durationText: String = "",
)

@Stable
data class ActiveSessionCompletedSectionsUiState(
    val items: List<CompletedSectionUiState> = emptyList()
)

@Stable
data class CompletedSectionUiState(
    val id: UUID = UUIDConverter.deadBeef,
    val name: String = "",
    val durationText: String = "",
    val color: Color = Color.Transparent,
)

@Stable
data class ActiveSessionEndDialogUiState(
    val rating: Int = 3,
    val comment: String = "",
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
    /** state is still unused */
    val activeTab: ActiveSessionTab = ActiveSessionTab.DEFAULT,
    val expanded: Boolean = false,
//    val metronomeState: MetronomeUiState,
//    val recorderState: RecorderUiState
)

enum class ActiveSessionTab {
    METRONOME, RECORDER, DEFAULT
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