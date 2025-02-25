/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText

data class TwoLinerData(
    val icon: UiIcon? = null,
    val firstLine: UiText? = null,
    val secondLine: UiText? = null,
    val trailingIcon: UiIcon? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun TwoLiner(
    modifier: Modifier = Modifier,
    data: TwoLinerData,
    paddingValues: PaddingValues = PaddingValues(
        horizontal = MaterialTheme.spacing.large,
        vertical = MaterialTheme.spacing.medium
    )
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .conditional(data.onClick != null) {
                clickable(onClick = { data.onClick?.invoke() })
            }
            .padding(paddingValues),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon
        data.icon?.let {
            Icon(imageVector = it.asIcon(), contentDescription = null)
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        }

        // Content
        Column {
            data.firstLine?.let {
                Text(
                    text = it.asString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            data.secondLine?.let {
                Text(
                    text = it.asString(),
                    fontSize = LocalTextStyle.current.fontSize * 0.9f,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    lineHeight = 1.2.em
                )
            }
        }

        // Trailing icon
        data.trailingIcon?.let {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = MaterialTheme.spacing.medium)
            )
            Icon(
                imageVector = it.asIcon(),
                contentDescription = data.firstLine?.asString() ?: data.secondLine?.asString()
            )
        }
    }
}
