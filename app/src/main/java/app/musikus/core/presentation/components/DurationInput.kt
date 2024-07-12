/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 *
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.settings.domain.ColorSchemeSelections

@Composable
fun DurationInput(
    modifier: Modifier = Modifier,
    hoursState: NumberInputState,
    minutesState: NumberInputState,
    onHoursChanged: (Int?) -> Unit = {},
    onMinutesChanged: (Int?) -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        NumberInput(
            state = hoursState,
            imeAction = ImeAction.Next,
            onValueChanged = onHoursChanged,
            label = { modifier ->
                Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        NumberInput(
            state = minutesState,
            imeAction = ImeAction.Done,
            onValueChanged = onMinutesChanged,
            label = { modifier ->
                Text(modifier = modifier, text = "m", style = MaterialTheme.typography.labelLarge)
            }
        )
    }
}


@MusikusPreviewElement1
@Composable
private fun DurationInputPreview(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        DurationInput(
            minutesState = rememberNumberInputState(initialValue = 42),
            hoursState = rememberNumberInputState(initialValue = 42),
        )
    }
}