/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.utils.UiText
import app.musikus.menu.domain.ColorSchemeSelections

@Composable
fun ToggleButton(
    modifier: Modifier = Modifier,
    options: List<ToggleButtonOption>,
    selected: ToggleButtonOption,
    onSelectedChanged: (ToggleButtonOption) -> Unit
) {
    Row(
        modifier = modifier,
    ) {
        options.forEachIndexed { index, option ->
            Button(
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == option) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (selected == option) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                ),
                shape = RoundedCornerShape(
                    topStartPercent = if (index == 0) 50 else 0,
                    bottomStartPercent = if (index == 0) 50 else 0,
                    topEndPercent = if (index == options.size - 1) 50 else 0,
                    bottomEndPercent = if (index == options.size - 1) 50 else 0
                ),
                onClick = { onSelectedChanged(option) }

            ) {
                Text(text = option.name.asString())
            }
        }
    }
}

data class ToggleButtonOption(
    val id: Int,
    val name: UiText
)



@MusikusPreviewElement1
@Composable
private fun PreviewGoalDialog(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        ToggleButton(
            options = listOf(
                ToggleButtonOption(0, UiText.DynamicString("Option 1")),
                ToggleButtonOption(1, UiText.DynamicString("Option 2")),
                ToggleButtonOption(2, UiText.DynamicString("Option 3")),
            ),
            selected = ToggleButtonOption(1, UiText.DynamicString("Option 2")),
            onSelectedChanged = {}
        )
    }
}