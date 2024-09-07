/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.presentation

import app.musikus.core.domain.GoalsSortMode
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.domain.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.library.data.daos.LibraryItem

typealias GoalsUiEventHandler = (GoalsUiEvent) -> Unit
typealias GoalDialogEventHandler = (GoalDialogUiEvent) -> Unit

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

    data class DialogUiEvent(val dialogEvent: GoalDialogUiEvent) : GoalsUiEvent()
}

sealed class GoalDialogUiEvent {
    data class HourTargetChanged(val hours: Int) : GoalDialogUiEvent()
    data class MinuteTargetChanged(val minutes: Int) : GoalDialogUiEvent()
    data class PeriodChanged(val period: Int) : GoalDialogUiEvent()
    data class PeriodUnitChanged(val periodUnit: GoalPeriodUnit) : GoalDialogUiEvent()
    data class GoalTypeChanged(val goalType: GoalType) : GoalDialogUiEvent()
    data class LibraryItemsSelected(val selectedLibraryItems: List<LibraryItem>) : GoalDialogUiEvent()
    data object Confirm : GoalDialogUiEvent()
    data object Dismiss : GoalDialogUiEvent()
}