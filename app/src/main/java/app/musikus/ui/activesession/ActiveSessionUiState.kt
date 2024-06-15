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
import app.musikus.database.daos.LibraryItem
import kotlinx.coroutines.flow.StateFlow
import java.time.ZonedDateTime
import java.util.UUID


@Stable
enum class ActiveSessionState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    UNKNOWN,
}

@Stable
/** Main UI State */
data class ActiveSessionUiState(
    val sessionState: StateFlow<ActiveSessionState>,
    val mainContentUiState: StateFlow<ActiveSessionContentUiState>,
    val newItemSelectorUiState: StateFlow<NewItemSelectorUiState?>,
    val dialogUiState: StateFlow<ActiveSessionDialogsUiState>,
)

@Stable
data class ActiveSessionDialogsUiState(
    val endDialogUiState: ActiveSessionEndDialogUiState?,
    val discardDialogVisible: Boolean,
)

@Stable
data class ActiveSessionContentUiState(
    val timerUiState: StateFlow<ActiveSessionTimerUiState>,
    val currentItemUiState: StateFlow<ActiveSessionCurrentItemUiState?>,
    val pastSectionsUiState: StateFlow<ActiveSessionCompletedSectionsUiState?>,
)

@Stable
data class ActiveSessionTimerUiState(
    val timerText: String,
    val subHeadingText: String,
)

@Stable
data class ActiveSessionCurrentItemUiState(
    val name: String,
    val color: Color,
    val durationText: String,
)

@Stable
data class ActiveSessionCompletedSectionsUiState(
    val items: List<CompletedSectionUiState>
)

@Stable
data class CompletedSectionUiState(
    val id: UUID,
    val name: String,
    val durationText: String,
    val color: Color,
)

@Stable
data class ActiveSessionEndDialogUiState(
    val rating: Int,
    val comment: String,
)

@Stable
data class NewItemSelectorUiState(
    val runningItem: LibraryItem?,
    val foldersWithItems: List<LibraryFolderWithItems>,
    val lastPracticedDates: Map<UUID, ZonedDateTime?>,
    val rootItems: List<LibraryItem>,
)

//@Stable
//data class ActiveSessionToolsUiState(
//    /** state is still unused */
//    val activeTab: ActiveSessionTab,
//    val expanded: Boolean,
//)

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

}

sealed class ActiveSessionEndDialogUiEvent {
    data class RatingChanged(val rating: Int) : ActiveSessionEndDialogUiEvent()
    data class CommentChanged(val comment: String) : ActiveSessionEndDialogUiEvent()
    data object Confirmed : ActiveSessionEndDialogUiEvent()
}