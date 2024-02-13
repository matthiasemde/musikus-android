/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.goals

import app.musikus.usecase.goals.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.utils.GoalsSortMode

typealias GoalsUiEventHandler = (GoalsUiEvent) -> Unit

sealed class GoalsUiEvent {
    data object BackButtonPressed : GoalsUiEvent()

    data class GoalPressed(
        val goal: GoalInstanceWithProgressAndDescriptionWithLibraryItems,
        val longClick: Boolean
    ) : GoalsUiEvent()

    data object GoalSortMenuPressed : GoalsUiEvent()
    data class GoalSortModeSelected(val mode: GoalsSortMode) : GoalsUiEvent()

    data object ArchiveButtonPressed : GoalsUiEvent()

    data object DeleteButtonPressed : GoalsUiEvent()
    data object DeleteOrArchiveDialogDismissed : GoalsUiEvent()
    data object DeleteOrArchiveDialogConfirmed : GoalsUiEvent()

    data object UndoButtonPressed : GoalsUiEvent()
    data object EditButtonPressed : GoalsUiEvent()

    data class AddGoalButtonPressed(val oneShot: Boolean) : GoalsUiEvent()

    data object ClearActionMode : GoalsUiEvent()

    data class GoalDialogUiEvent(val dialogEvent: GoalDialogUiEvent) : GoalsUiEvent()
}

