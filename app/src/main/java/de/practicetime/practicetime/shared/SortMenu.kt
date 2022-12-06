/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.shared

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import de.practicetime.practicetime.datastore.SortDirection

@Composable
fun <T> SortMenu(
    show: Boolean,
    sortModes: List<T>,
    currentSortMode: T,
    currentSortDirection: SortDirection,
    label: (T) -> String,
    onShowMenuChanged: (Boolean) -> Unit,
    onSelectionHandler: (T) -> Unit
) {
    TextButton(
        onClick = { onShowMenuChanged(!show) })
    {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            text = label(currentSortMode)
        )
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = when (currentSortDirection) {
                SortDirection.ASCENDING -> Icons.Default.ArrowUpward
                SortDirection.DESCENDING -> Icons.Default.ArrowDownward
            },
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null
        )
        DropdownMenu(
            offset = DpOffset((-10).dp, 10.dp),
            expanded = show,
            onDismissRequest = { onShowMenuChanged(false) },
        ) {
            // Menu Header
            Text(
                modifier = Modifier.padding(12.dp),
                text = "Sort by",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Menu Body
            val directionIcon: @Composable () -> Unit = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = when (currentSortDirection) {
                        SortDirection.ASCENDING -> Icons.Default.ArrowUpward
                        SortDirection.DESCENDING -> Icons.Default.ArrowDownward
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }
            sortModes.forEach { sortMode ->
                val selected = sortMode == currentSortMode
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label(sortMode),
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else Color.Unspecified
                        )
                    },
                    onClick = { onSelectionHandler(sortMode) },
                    trailingIcon = if (selected) directionIcon else null
                )
            }
        }
    }
}
