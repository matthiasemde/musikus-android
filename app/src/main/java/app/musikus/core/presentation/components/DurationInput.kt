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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
fun DurationInput(
    value: Duration,
    onValueChanged: (Duration) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        var hours = value.inWholeHours.toString().padStart(2, '0')
        var minutes = (value - value.inWholeHours.hours).inWholeMinutes.toString().padStart(2, '0')

        NumberInput(
            value = hours,
            onValueChange = {
                hours = it
                onValueChanged(
                    (hours.toIntOrNull() ?: 0).hours +
                            (minutes.toIntOrNull() ?: 0).minutes
                )
            },
            showLeadingZero = true,
            textSize = 40.sp,
            maxValue = 99,
            imeAction = ImeAction.Next,
            label = { modifier ->
                Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        NumberInput(
            value = minutes,
            onValueChange = {
                minutes = it
                onValueChanged(
                    (hours.toIntOrNull() ?: 0).hours +
                            (minutes.toIntOrNull() ?: 0).minutes
                )
            },
            showLeadingZero = true,
            textSize = 40.sp,
            maxValue = 59,
            imeAction = ImeAction.Done,
            label = { modifier ->
                Text(modifier = modifier, text = "m", style = MaterialTheme.typography.labelLarge)
            }
        )
    }
}