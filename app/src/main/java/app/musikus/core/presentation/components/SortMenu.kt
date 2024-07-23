/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortMode

@Composable
fun <T : SortMode<*>> SortMenu(
    modifier: Modifier = Modifier,
    show: Boolean,
    sortItemDescription: String,
    sortModes: List<T>,
    currentSortMode: T,
    currentSortDirection: SortDirection,
    onShowMenuChanged: (Boolean) -> Unit,
    onSelectionHandler: (T) -> Unit
) {
    val mainContentDescription = stringResource(id = R.string.components_sort_menu_content_description, sortItemDescription)
    val dropdownContentDescription = stringResource(id = R.string.components_sort_menu_dropdown_content_description, sortItemDescription)
    TextButton(
        modifier = modifier.semantics {
            contentDescription = mainContentDescription
        },
        onClick = { onShowMenuChanged(!show) })
    {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            text = currentSortMode.label
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
            modifier = Modifier.semantics {
                contentDescription = dropdownContentDescription
            },
            offset = DpOffset((-10).dp, 10.dp),
            expanded = show,
            onDismissRequest = { onShowMenuChanged(false) },
        ) {
            // Menu Header
            Text(
                modifier = Modifier.padding(12.dp),
                text = stringResource(id = R.string.components_sort_menu_title),
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
                            text = sortMode.label,
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