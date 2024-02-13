/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.goals

import app.musikus.database.daos.LibraryItem
import app.musikus.ui.components.TopBarUiState
import app.musikus.ui.library.DialogMode
import app.musikus.usecase.goals.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.SortDirection
import java.util.UUID

data class GoalsSortMenuUiState(
    val show: Boolean,

    val mode: GoalsSortMode,
    val direction: SortDirection,
)

data class GoalsTopBarUiState(
    override val title: String,
    override val showBackButton: Boolean,
    val sortMenuUiState: GoalsSortMenuUiState,
) : TopBarUiState

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

data class GoalsAddOrEditDialogUiState(
    val mode: DialogMode,
    val goalToEditId: UUID?,
    val dialogData: GoalDialogData,
    val libraryItems: List<LibraryItem>,
)

data class GoalsDeleteOrArchiveDialogUiState(
    val isArchiveAction: Boolean,
    val numberOfSelections: Int,
)

data class GoalsDialogUiState(
    val addOrEditDialogUiState: GoalsAddOrEditDialogUiState?,
    val deleteOrArchiveDialogUiState: GoalsDeleteOrArchiveDialogUiState?,
)

data class GoalsUiState (
    val topBarUiState: GoalsTopBarUiState,
    val actionModeUiState: GoalsActionModeUiState,
    val contentUiState: GoalsContentUiState,
    val dialogUiState: GoalsDialogUiState,
)