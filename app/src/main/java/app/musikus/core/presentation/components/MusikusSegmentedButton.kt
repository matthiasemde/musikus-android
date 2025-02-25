/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiText
import app.musikus.menu.domain.ColorSchemeSelections

@Composable
fun MusikusSegmentedButton(
    modifier: Modifier = Modifier,
    options: List<SegmentedButtonOption>,
    selected: SegmentedButtonOption,
    onSelectedChanged: (SegmentedButtonOption) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, toggleButton ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                selected = index == options.indexOf(selected),
                onClick = {
                    onSelectedChanged(toggleButton)
                },
                label = {
                    Text(
                        text = toggleButton.name.asString(),
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall),
                        softWrap = false
                    )
                }
            )
        }
    }
}

data class SegmentedButtonOption(
    val id: Int,
    val name: UiText
)

@MusikusPreviewElement1
@Composable
private fun PreviewMusikusSegmentedButton(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme = theme) {
        MusikusSegmentedButton(
            options = listOf(
                SegmentedButtonOption(0, UiText.DynamicString("Option 1")),
                SegmentedButtonOption(1, UiText.DynamicString("Option 2")),
                SegmentedButtonOption(2, UiText.DynamicString("Option 3")),
            ),
            selected = SegmentedButtonOption(1, UiText.DynamicString("Option 2")),
            onSelectedChanged = {}
        )
    }
}
