/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 */

package app.musikus.goals.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.presentation.components.IntSelectionSpinnerOption
import app.musikus.core.presentation.components.NumberInput
import app.musikus.core.presentation.components.NumberInputState
import app.musikus.core.presentation.components.SelectionSpinner
import app.musikus.core.presentation.components.rememberNumberInputState
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.utils.TestTags
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.menu.domain.ColorSchemeSelections

/**
 * Input field for selecting the period (DAY, WEEKS, MONTHS, ...) and its amount.
 *
 * Amount is input via a NumberInput field. The period unit is selected via a SelectionSpinner.
 */
@Composable
fun PeriodInput(
    modifier: Modifier = Modifier,
    periodAmountInputState: NumberInputState,
    periodUnitSelectionState: PeriodUnitSelectionState,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberInput(
            state = periodAmountInputState,
            imeAction = ImeAction.Next,
            padStart = false,
            textStyle = MaterialTheme.typography.titleLarge,
            contentDescr = stringResource(id = R.string.goals_goal_dialog_period_amount_content_description),
        )

        Spacer(modifier = Modifier.width(8.dp))

        val selectedUnit = periodUnitSelectionState.currentSelection.value
        SelectionSpinner(
            modifier = Modifier.width(130.dp),
            // map the SelectionSpinnerOption based on its id to the GoalPeriodUnit ordinal
            options = periodUnitSelectionState.allowedPeriodUnits.map {
                IntSelectionSpinnerOption(
                    it.ordinal,
                    it.toUiText(quantity = periodAmountInputState.currentValue.value ?: 0)
                )
            },
            selectedOption = IntSelectionSpinnerOption(
                selectedUnit.ordinal,
                selectedUnit.toUiText(
                    quantity = periodAmountInputState.currentValue.value ?: 0
                )
            ),
            semanticDescription = stringResource(id = R.string.goals_goal_dialog_period_input_description),
            dropdownTestTag = TestTags.GOAL_DIALOG_PERIOD_UNIT_SELECTOR_DROPDOWN,
            onSelectedChange = { selection ->
                // re-map the SelectionSpinnerOption based on its id to the GoalPeriodUnit ordinal
                periodUnitSelectionState.currentSelection.value =
                    GoalPeriodUnit.entries[(selection as IntSelectionSpinnerOption).id ?: 0]
            }
        )
    }
}

/**
 * State for the period selection spinner element.
 */
@Stable
class PeriodUnitSelectionState(
    initialPeriodUnit: GoalPeriodUnit,
    val allowedPeriodUnits: List<GoalPeriodUnit>,
) {
    init {
        require(allowedPeriodUnits.contains(initialPeriodUnit)) {
            "The initial period unit must be one of the allowed period units."
        }
    }

    // the current selected period unit
    val currentSelection = mutableStateOf(initialPeriodUnit)

    /**
     * Saver to support rememberSaveable / Bundle saving and restoring.
     */
    companion object {
        fun Saver(): Saver<PeriodUnitSelectionState, *> = Saver(
            save = {
                listOf(
                    it.currentSelection.value,
                    it.allowedPeriodUnits,
                )
            },
            restore = {
                PeriodUnitSelectionState(
                    initialPeriodUnit = it[0] as GoalPeriodUnit,
                    allowedPeriodUnits = (it[1] as List<*>).filterIsInstance<GoalPeriodUnit>(),
                )
            }
        )
    }
}

/**
 * RememberSaveable wrapper for rememberPeriodUnitSelectionState.
 * Use to generate a PeriodUnitSelectionState instance which survives configuration changes.
 */
@Composable
fun rememberPeriodUnitSelectionState(
    initialPeriodUnit: GoalPeriodUnit,
    allowedPeriodUnits: List<GoalPeriodUnit>,
) = rememberSaveable(saver = PeriodUnitSelectionState.Saver()) {
    PeriodUnitSelectionState(
        initialPeriodUnit = initialPeriodUnit,
        allowedPeriodUnits = allowedPeriodUnits,
    )
}

@MusikusPreviewElement1
@Composable
private fun PeriodInputPreview(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        PeriodInput(
            periodAmountInputState = rememberNumberInputState(initialValue = 42, maxValue = 99),
            periodUnitSelectionState = rememberPeriodUnitSelectionState(
                initialPeriodUnit = GoalPeriodUnit.DAY,
                allowedPeriodUnits = GoalPeriodUnit.entries.toList()
            )
        )
    }
}
