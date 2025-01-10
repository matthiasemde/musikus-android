/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.presentation

import app.musikus.core.domain.SortDirection
import app.musikus.goals.data.GoalsSortMode
import app.musikus.goals.domain.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.presentation.DialogMode
import java.util.UUID

data class GoalsSortMenuUiState(
    val show: Boolean,

    val mode: GoalsSortMode,
    val direction: SortDirection,
)

data class GoalsTopBarUiState(
    val sortMenuUiState: GoalsSortMenuUiState,
)

data class GoalsActionModeUiState(
    val isActionMode: Boolean,
    val numberOfSelections: Int,
    val showEditAction: Boolean,
)

data class GoalsContentUiState(
    val currentGoals: List<GoalInstanceWithProgressAndDescriptionWithLibraryItems>,
    val selectedGoalIds: Set<UUID>,

    val showHint: Boolean,
)

/**
 * UI state for the dialog which is displayed when adding or changing a goal.
 */
data class GoalsAddOrEditDialogUiState(
    val mode: DialogMode,
    val oneShotGoal: Boolean,
    val goalToEditId: UUID?,
    val initialTargetHours: Int,
    val initialTargetMinutes: Int,
    val libraryItems: List<LibraryItem>,
)

data class GoalsDeleteOrArchiveDialogUiState(
    val isArchiveAction: Boolean,
    val numberOfSelections: Int,
)

/**
 * Container for both dialogs that can be shown in the goals screen.
 */
data class GoalsDialogUiState(
    val addOrEditDialogUiState: GoalsAddOrEditDialogUiState?,
    val deleteOrArchiveDialogUiState: GoalsDeleteOrArchiveDialogUiState?,
)

data class GoalsUiState(
    val topBarUiState: GoalsTopBarUiState,
    val actionModeUiState: GoalsActionModeUiState,
    val contentUiState: GoalsContentUiState,
    val dialogUiState: GoalsDialogUiState,
)
