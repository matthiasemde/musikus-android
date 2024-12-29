/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.goals.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.window.Dialog
import app.musikus.R
import app.musikus.core.presentation.components.DialogActions
import app.musikus.core.presentation.components.DialogHeader
import app.musikus.core.presentation.components.DurationInput
import app.musikus.core.presentation.components.ToggleButton
import app.musikus.core.presentation.components.ToggleButtonOption
import app.musikus.core.presentation.components.rememberNumberInputState
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalType
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.presentation.DialogMode
import app.musikus.menu.domain.ColorSchemeSelections
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

typealias GoalDialogUiEventHandler = (GoalDialogUiEvent) -> Boolean

sealed class GoalDialogUiEvent {
    // callback when the duration of the goal is changed
    data class TargetChanged(val target: Duration) : GoalDialogUiEvent()
    // callback when the period of the goal is changed
    data class PeriodChanged(val period: Int) : GoalDialogUiEvent()
    data class PeriodUnitChanged(val periodUnit: GoalPeriodUnit) : GoalDialogUiEvent()
    data class GoalTypeChanged(val goalType: GoalType) : GoalDialogUiEvent()
    data class LibraryItemsSelected(val selectedLibraryItems: List<LibraryItem>) : GoalDialogUiEvent()
    data object Confirm : GoalDialogUiEvent()
    data object Dismiss : GoalDialogUiEvent()
}

@Composable
fun GoalDialog(
    uiState: GoalsAddOrEditDialogUiState,
    eventHandler: GoalDialogUiEventHandler,
) {
    /**
     * Passed uiState
     */
    val isEditMode = uiState.mode == DialogMode.EDIT
    val dialogData = uiState.dialogData

    /**
     * Local GoalDialog state objects (internal Compose State)
     */

    // state for the HOURS duration input field
    val hoursState = rememberNumberInputState(
        initialValue = dialogData.target.inWholeHours.toInt(),
        minValue = 0,
        maxValue = 99
    )
    // state for the MINUTES duration input field
    val minutesState = rememberNumberInputState(
        initialValue = (dialogData.target - dialogData.target.inWholeHours.hours).inWholeMinutes.toInt(),
        minValue = 0,
        maxValue = 59
    )

    // state for the period amount input field
    val periodAmountInputState = rememberNumberInputState(
        initialValue = dialogData.periodInPeriodUnits,
        minValue = 1,
        maxValue = 99
    )

    // state for the period unit selection
    val periodUnitSelectionState = rememberPeriodUnitSelectionState(
            initialPeriodUnit = dialogData.periodUnit,
            allowedPeriodUnits = GoalPeriodUnit.entries.toList()
    )

    Dialog(
        onDismissRequest = { eventHandler(GoalDialogUiEvent.Dismiss) },
    ) {
        // logic for enabling the confirm button only on valid and complete input
        var confirmButtonEnabled = true

        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialogHeader(
                title = stringResource(
                    id =
                    if (isEditMode) {
                        R.string.goals_goal_dialog_title_edit
                    } else {
                        R.string.goals_goal_dialog_title
                    }
                )
            )
            DurationInput(
                modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                hoursState = hoursState,
                minutesState = minutesState,
            )
            confirmButtonEnabled = confirmButtonEnabled &&      // https://stackoverflow.com/a/55404768
                    (hoursState.currentValue.value ?: 0) > 0 || (minutesState.currentValue.value ?: 0) > 0

            if (!isEditMode) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                PeriodInput(
                    periodAmountInputState = periodAmountInputState,
                    periodUnitSelectionState = periodUnitSelectionState,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                HorizontalDivider(Modifier.padding(horizontal = MaterialTheme.spacing.large))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                ToggleButton(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large),
                    options = GoalType.entries.map {
                        ToggleButtonOption(it.ordinal, it.toUiText())
                    },
                    selected = ToggleButtonOption(
                        dialogData.goalType.ordinal,
                        dialogData.goalType.toUiText()
                    ),
                    onSelectedChanged = { option ->
                        eventHandler(GoalDialogUiEvent.GoalTypeChanged(GoalType.entries[option.id]))
                    }
                )

//                if (dialogData.goalType == GoalType.ITEM_SPECIFIC) {
                if (false) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    val libraryItems = uiState.libraryItems

                    if (libraryItems.isNotEmpty()) {
//                        SelectionSpinner(
//                            placeholder = {
//                                Text(
//                                    text = stringResource(
//                                        id = R.string.goals_goal_dialog_item_selector_placeholder
//                                    )
//                                )
//                            },
//                            options = libraryItems.map {
//                                UUIDSelectionSpinnerOption(it.id, UiText.DynamicString(it.name))
//                            },
//                            selectedOption = dialogData.selectedLibraryItems.firstOrNull()?.let {
//                                UUIDSelectionSpinnerOption(it.id, UiText.DynamicString(it.name))
//                            },
//                            semanticDescription = stringResource(
//                                id = R.string.goals_goal_dialog_item_selector_description
//                            ),
//                            dropdownTestTag = TestTags.GOAL_DIALOG_ITEM_SELECTOR_DROPDOWN,
//                            onExpandedChange = {
//                                libraryItemsSelectorExpanded = it
//                                periodUnitSelectorExpanded = false
//                            },
//                            onSelectedChange = { selection ->
//                                eventHandler(
//                                    GoalDialogUiEvent.LibraryItemsSelected(
//                                        libraryItems.filter {
//                                            it.id == (selection as UUIDSelectionSpinnerOption).id
//                                        }
//                                    )
//                                )
//                                libraryItemsSelectorExpanded = false
//                            }
//                        )
                    } else {
                        Text(text = stringResource(id = R.string.goals_goal_dialog_item_selector_no_items))
                    }
                }

                confirmButtonEnabled = confirmButtonEnabled && (
                    dialogData.goalType == GoalType.NON_SPECIFIC ||
                        dialogData.selectedLibraryItems.isNotEmpty()
                    )
            }

            DialogActions(
                onConfirmHandler = { eventHandler(GoalDialogUiEvent.Confirm) },
                onDismissHandler = { eventHandler(GoalDialogUiEvent.Dismiss) },
                confirmButtonEnabled = confirmButtonEnabled,
                confirmButtonText = stringResource(
                    id =
                    if (isEditMode) {
                        R.string.goals_goal_dialog_confirm_edit
                    } else {
                        R.string.goals_goal_dialog_confirm
                    }
                )
            )
        }
    }
}



@MusikusPreviewElement1
@Composable
private fun PreviewGoalDialog(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        GoalDialog(
            uiState = GoalsAddOrEditDialogUiState(
                mode = DialogMode.ADD,
                libraryItems = emptyList(),
                goalToEditId = null,
                dialogData = GoalDialogData(
                    target = 10.seconds,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    goalType = GoalType.NON_SPECIFIC,
                    selectedLibraryItems = emptyList()
                ),
            ),
            eventHandler = { true }
        )
    }
}