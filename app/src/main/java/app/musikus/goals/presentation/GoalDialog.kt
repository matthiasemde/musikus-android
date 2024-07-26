/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 */

package app.musikus.goals.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.musikus.R
import app.musikus.core.presentation.components.DialogActions
import app.musikus.core.presentation.components.DialogHeader
import app.musikus.core.presentation.components.DurationInput
import app.musikus.core.presentation.components.IntSelectionSpinnerOption
import app.musikus.core.presentation.components.MyToggleButton
import app.musikus.core.presentation.components.NumberInput
import app.musikus.core.presentation.components.NumberInputState
import app.musikus.core.presentation.components.SelectionSpinner
import app.musikus.core.presentation.components.ToggleButtonOption
import app.musikus.core.presentation.components.UUIDSelectionSpinnerOption
import app.musikus.core.presentation.components.rememberNumberInputState
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.TestTags
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalType
import app.musikus.library.presentation.DialogMode
import app.musikus.settings.domain.ColorSchemeSelections
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@Composable
fun GoalDialog(
    uiState: GoalsAddOrEditDialogUiState,
    eventHandler: GoalDialogEventHandler,
) {
    /**
     * Local GoalDialog state
     */

    var libraryItemsSelectorExpanded by remember { mutableStateOf(false) }
    var periodUnitSelectorExpanded by remember { mutableStateOf(false) }

    val dialogData = uiState.dialogData

    val hoursState = rememberNumberInputState(
        initialValue = dialogData.target.inWholeHours.toInt(),
        minValue = 0,
        maxValue = 99
    )
    val minutesState = rememberNumberInputState(
        initialValue = (dialogData.target - dialogData.target.inWholeHours.hours).inWholeMinutes.toInt(),
        minValue = 0,
        maxValue = 59
    )

    val periodInputState = rememberNumberInputState(
        initialValue = dialogData.periodInPeriodUnits,
        minValue = 0,
        maxValue = 99
    )

    val isEditMode = uiState.mode == DialogMode.EDIT

    Dialog(
        onDismissRequest = { eventHandler(GoalDialogUiEvent.Dismiss) },
    ) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialogHeader(
                title = stringResource(
                    id = if (isEditMode) R.string.goalDialogTitleEdit else R.string.addGoalDialogTitle
                )
            )

            var confirmButtonEnabled = true
            DurationInput(
                modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                hoursState = hoursState,
                minutesState = minutesState,
                onHoursChanged = { eventHandler(GoalDialogUiEvent.HourTargetChanged(it)) },
                onMinutesChanged = { eventHandler(GoalDialogUiEvent.MinuteTargetChanged(it)) },
            )
            confirmButtonEnabled = confirmButtonEnabled && dialogData.target > 0.seconds

            if (!isEditMode) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                PeriodInput(
                    periodNumberInputState = periodInputState,
                    periodOptions = GoalPeriodUnit.entries.toList(),
                    initialSelection = dialogData.periodUnit,
                    onPeriodChanged = { eventHandler(GoalDialogUiEvent.PeriodChanged(it ?: 0)) },
                    onPeriodUnitChanged = { eventHandler(GoalDialogUiEvent.PeriodUnitChanged(it)) },
                )
                confirmButtonEnabled = confirmButtonEnabled && dialogData.periodInPeriodUnits > 0

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                HorizontalDivider(Modifier.padding(horizontal = MaterialTheme.spacing.large))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                MyToggleButton(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large),
                    options = GoalType.entries.map {
                        ToggleButtonOption(it.ordinal, it.toString())
                    },
                    selected = ToggleButtonOption(
                        dialogData.goalType.ordinal,
                        dialogData.goalType.toString()
                    ),
                    onSelectedChanged = { option ->
                        eventHandler(GoalDialogUiEvent.GoalTypeChanged(GoalType.entries[option.id]))
                    }
                )

                if (dialogData.goalType == GoalType.ITEM_SPECIFIC) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    val libraryItems = uiState.libraryItems

                    if (libraryItems.isNotEmpty()) {
                        SelectionSpinner(
                            placeholder = { Text(text = "Select a library item") },
                            options = libraryItems.map {
                                UUIDSelectionSpinnerOption(it.id, it.name)
                            },
                            selectedOption = dialogData.selectedLibraryItems.firstOrNull()?.let {
                                UUIDSelectionSpinnerOption(it.id, it.name)
                            },
                            semanticDescription = "Select library item",
                            dropdownTestTag = TestTags.GOAL_DIALOG_ITEM_SELECTOR_DROPDOWN,
                            onSelectedChange = { selection ->
                                eventHandler(GoalDialogUiEvent.LibraryItemsSelected(libraryItems.filter {
                                    it.id == (selection as UUIDSelectionSpinnerOption).id
                                }))
                                libraryItemsSelectorExpanded = false
                            }
                        )
                    } else {
                        Text(text = "No items in your library.")
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
                confirmButtonText = if (isEditMode) "Edit" else "Create"
            )
        }
    }
}

@Composable
fun PeriodInput(
    periodNumberInputState: NumberInputState,
    periodOptions: List<GoalPeriodUnit>,
    initialSelection: GoalPeriodUnit,
    onPeriodChanged: (Int?) -> Unit,
    onPeriodUnitChanged: (GoalPeriodUnit) -> Unit,
) {
    var selectedPeriod by remember { mutableStateOf(initialSelection) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "in")
        Spacer(modifier = Modifier.width(8.dp))
        NumberInput(
            state = periodNumberInputState,
            imeAction = ImeAction.Done,
            textStyle = MaterialTheme.typography.titleLarge,
            padStart = false,
            onValueChanged = onPeriodChanged,
        )
        Spacer(modifier = Modifier.width(8.dp))

        SelectionSpinner(
            modifier = Modifier.width(130.dp),
            options = periodOptions.map {
                IntSelectionSpinnerOption(
                    it.ordinal, // id is the ordinal of the GoalPeriodUnit
                    it.toString()
                )
            },
            selectedOption = IntSelectionSpinnerOption(selectedPeriod.ordinal, selectedPeriod.toString()),
            semanticDescription = "Select period unit",
            dropdownTestTag = TestTags.GOAL_DIALOG_PERIOD_UNIT_SELECTOR_DROPDOWN,
            onSelectedChange = { selection ->
                selectedPeriod = GoalPeriodUnit.entries[(selection as IntSelectionSpinnerOption?)?.id ?: 0]
                onPeriodUnitChanged(selectedPeriod)
            }
        )
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
            eventHandler = { }
        )
    }
}