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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.musikus.R
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.ui.components.DialogActions
import app.musikus.ui.components.DialogHeader
import app.musikus.ui.components.DurationInput
import app.musikus.ui.components.IntSelectionSpinnerOption
import app.musikus.ui.components.MyToggleButton
import app.musikus.ui.components.NumberInput
import app.musikus.ui.components.SelectionSpinner
import app.musikus.ui.components.ToggleButtonOption
import app.musikus.ui.components.UUIDSelectionSpinnerOption
import app.musikus.library.presentation.DialogMode
import app.musikus.core.presentation.theme.spacing
import app.musikus.utils.TestTags
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

typealias GoalDialogEventHandler = (GoalDialogUiEvent) -> Unit

sealed class GoalDialogUiEvent {
    data class TargetChanged(val target: Duration) : GoalDialogUiEvent()
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
    eventHandler: GoalDialogEventHandler,
) {
    /**
     * Local GoalDialog state
     */

    var libraryItemsSelectorExpanded by remember { mutableStateOf(false) }
    var periodUnitSelectorExpanded by remember { mutableStateOf(false) }


    /**
     * Composing the Dialog
     */

    val dialogData = uiState.dialogData

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
                    id = if(isEditMode) R.string.goalDialogTitleEdit else R.string.addGoalDialogTitle
                )
            )

            var confirmButtonEnabled = true
            DurationInput(
                value = dialogData.target,
                onValueChanged = { eventHandler(GoalDialogUiEvent.TargetChanged(it)) }
            )
            confirmButtonEnabled = confirmButtonEnabled && dialogData.target > 0.seconds

            if(!isEditMode) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                PeriodInput(
                    periodInPeriodUnits = dialogData.periodInPeriodUnits,
                    periodUnit = dialogData.periodUnit,
                    periodUnitSelectorExpanded = periodUnitSelectorExpanded,
                    onPeriodChanged = { eventHandler(GoalDialogUiEvent.PeriodChanged(it)) },
                    onPeriodUnitChanged = {
                        eventHandler(GoalDialogUiEvent.PeriodUnitChanged(it))
                        periodUnitSelectorExpanded = false
                    },
                    onPeriodUnitSelectorExpandedChanged = {
                        periodUnitSelectorExpanded = it
                        libraryItemsSelectorExpanded = false
                    }
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
                            expanded = libraryItemsSelectorExpanded,
                            placeholder = { Text(text = "Select a library item") },
                            options = libraryItems.map {
                                UUIDSelectionSpinnerOption(it.id, it.name)
                            },
                            selected = dialogData.selectedLibraryItems.firstOrNull()?.let {
                                UUIDSelectionSpinnerOption(it.id, it.name)
                            },
                            semanticDescription = "Select library item",
                            dropdownTestTag = TestTags.GOAL_DIALOG_ITEM_SELECTOR_DROPDOWN,
                            onExpandedChange = {
                                libraryItemsSelectorExpanded = it
                                periodUnitSelectorExpanded = false
                            },
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
    periodInPeriodUnits: Int,
    periodUnit: GoalPeriodUnit,
    periodUnitSelectorExpanded: Boolean,
    onPeriodChanged: (Int) -> Unit,
    onPeriodUnitChanged: (GoalPeriodUnit) -> Unit,
    onPeriodUnitSelectorExpandedChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "in")
        Spacer(modifier = Modifier.width(8.dp))
        NumberInput(
            value = periodInPeriodUnits.toString(),
            onValueChange = { onPeriodChanged(it.toIntOrNull() ?: 0) },
            textSize = 20.sp,
            maxValue = 99,
            imeAction = ImeAction.Next,
        )
        Spacer(modifier = Modifier.width(8.dp))
        SelectionSpinner(
            modifier = Modifier.width(130.dp),
            expanded = periodUnitSelectorExpanded,
            options = GoalPeriodUnit.entries.map { IntSelectionSpinnerOption(it.ordinal, it.toString()) },
            selected = IntSelectionSpinnerOption(periodUnit.ordinal, periodUnit.toString()),
            semanticDescription = "Select period unit",
            dropdownTestTag = TestTags.GOAL_DIALOG_PERIOD_UNIT_SELECTOR_DROPDOWN,
            onExpandedChange = onPeriodUnitSelectorExpandedChanged,
            onSelectedChange = { selection ->
                onPeriodUnitChanged(GoalPeriodUnit.entries[(selection as IntSelectionSpinnerOption?)?.id ?: 0])
                onPeriodUnitSelectorExpandedChanged(false)
            }
        )
    }
}