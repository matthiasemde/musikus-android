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
import app.musikus.core.presentation.components.SelectionSpinnerState
import app.musikus.core.presentation.components.rememberNumberInputState
import app.musikus.core.presentation.components.rememberSelectionSpinnerState
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
    periodUnitSelectionState: SelectionSpinnerState,
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

        SelectionSpinner(
            modifier = Modifier.width(130.dp),
            spinnerState = periodUnitSelectionState,
            semanticDescription = stringResource(id = R.string.goals_goal_dialog_period_input_description),
            dropdownTestTag = TestTags.GOAL_DIALOG_PERIOD_UNIT_SELECTOR_DROPDOWN,
        )
    }
}

@MusikusPreviewElement1
@Composable
private fun PeriodInputPreview(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        PeriodInput(
            periodAmountInputState = rememberNumberInputState(initialValue = 42, maxValue = 99),
            periodUnitSelectionState = rememberSelectionSpinnerState(
                options = GoalPeriodUnit.entries.map{ IntSelectionSpinnerOption(it.ordinal, it.toUiText()) }
            )
        )
    }
}
