/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiIcon

@Composable
fun DialogHeader(
    title: String,
    icon: UiIcon? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.size(24.dp).align(Alignment.CenterVertically),
                    imageVector = icon.asIcon(),
                    contentDescription = "jksdshf",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Spacer(Modifier.width(MaterialTheme.spacing.small))
            }

            Text(
                modifier = Modifier.alignByBaseline(),
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
    }
}