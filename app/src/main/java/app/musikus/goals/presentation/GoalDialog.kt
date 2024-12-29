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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.musikus.core.presentation.components.SelectionSpinner
import app.musikus.core.presentation.components.ToggleButton
import app.musikus.core.presentation.components.ToggleButtonOption
import app.musikus.core.presentation.components.UUIDSelectionSpinnerOption
import app.musikus.core.presentation.components.rememberNumberInputState
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.TestTags
import app.musikus.core.presentation.utils.UiText
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
    data class Confirm(
        val target: Duration,
        val period: Int,
        val periodUnit: GoalPeriodUnit,
        val goalType: GoalType,
        val selectedLibraryItems: List<LibraryItem>
    ) : GoalDialogUiEvent()

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

    /**
     * Local GoalDialog state objects (internal Compose State)
     */

    // state for the HOURS duration input field
    val hoursState = rememberNumberInputState(
        initialValue = uiState.initialTargetHours,
        minValue = 0,
        maxValue = 99
    )
    // state for the MINUTES duration input field
    val minutesState = rememberNumberInputState(
        initialValue = uiState.initialTargetMinutes,
        minValue = 0,
        maxValue = 59
    )

    // state for the period amount input field
    val periodAmountInputState = rememberNumberInputState(
        initialValue = 1,
        minValue = 1,
        maxValue = 99
    )

    // state for the period unit selection
    val periodUnitSelectionState = rememberPeriodUnitSelectionState(
        initialPeriodUnit = GoalPeriodUnit.DEFAULT,
        allowedPeriodUnits = GoalPeriodUnit.entries.toList()
    )

    val selectedGoalType = remember { mutableStateOf(GoalType.DEFAULT) }
    val selectedLibraryItems: MutableState<List<LibraryItem>> = remember { mutableStateOf(emptyList()) }

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

                val toggleButtonOptions = GoalType.entries.map {
                    ToggleButtonOption(it.ordinal, it.toUiText())
                }

                ToggleButton(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large),
                    options = toggleButtonOptions,
                    selected = toggleButtonOptions[selectedGoalType.value.ordinal],
                    onSelectedChanged = { option ->
                        selectedGoalType.value = GoalType.entries[option.id]
                    }
                )

                if (selectedGoalType.value == GoalType.ITEM_SPECIFIC) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    val libraryItems = uiState.libraryItems

                    if (libraryItems.isNotEmpty()) {
                        SelectionSpinner(
                            placeholder = {
                                Text(
                                    text = stringResource(
                                        id = R.string.goals_goal_dialog_item_selector_placeholder
                                    )
                                )
                            },
                            options = libraryItems.map {
                                UUIDSelectionSpinnerOption(it.id, UiText.DynamicString(it.name))
                            },
                            // select the first item if it is not null
                            selectedOption = selectedLibraryItems.value.firstOrNull()?.let {
                                UUIDSelectionSpinnerOption(it.id, UiText.DynamicString(it.name))
                            },
                            semanticDescription = stringResource(id = R.string.goals_goal_dialog_item_selector_description),
                            dropdownTestTag = TestTags.GOAL_DIALOG_ITEM_SELECTOR_DROPDOWN,
                            onSelectedChange = { selection ->
                                selectedLibraryItems.value = libraryItems.filter { it.id == (selection as UUIDSelectionSpinnerOption).id }
                            }
                        )
                    } else {
                        Text(text = stringResource(id = R.string.goals_goal_dialog_item_selector_no_items))
                    }
                }
                confirmButtonEnabled =
                    confirmButtonEnabled && (selectedGoalType.value == GoalType.NON_SPECIFIC || selectedLibraryItems.value.isNotEmpty())
            }

            DialogActions(
                onConfirmHandler = {
                    eventHandler(
                        GoalDialogUiEvent.Confirm(
                            target = (hoursState.currentValue.value!!).hours + (minutesState.currentValue.value!!).seconds,
                            period = periodAmountInputState.currentValue.value!!,
                            periodUnit = periodUnitSelectionState.currentSelection.value,
                            goalType = selectedGoalType.value,
                            selectedLibraryItems = selectedLibraryItems.value
                        )
                    )
                },
                onDismissHandler = { eventHandler(GoalDialogUiEvent.Dismiss) },
                confirmButtonEnabled = confirmButtonEnabled,
                confirmButtonText = stringResource(
                    id = if (isEditMode) {
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
                initialTargetHours = 0,
                initialTargetMinutes = 0,
            ),
            eventHandler = { true }
        )
    }
}