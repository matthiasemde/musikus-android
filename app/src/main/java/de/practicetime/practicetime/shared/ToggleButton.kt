/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.shared

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class ToggleButtonOption(
    val id: Int,
    val name: String
)

@Composable
fun MyToggleButton(
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
                    containerColor = if(selected == option)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if(selected == option)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                shape = RoundedCornerShape(
                    topStartPercent = if(index == 0) 50 else 0,
                    bottomStartPercent = if(index == 0) 50 else 0,
                    topEndPercent = if (index == options.size - 1) 50 else 0,
                    bottomEndPercent = if (index == options.size - 1) 50 else 0
                ),
                onClick = { onSelectedChanged(option) }

            ) {
                Text(text = option.name)
            }
        }
    }
}