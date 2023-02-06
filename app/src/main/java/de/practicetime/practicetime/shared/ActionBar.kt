/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

interface ActionModeUiState {
    val isActionMode: Boolean
    val numberOfSelections: Int
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBar(
    numSelectedItems: Int,
    onDismissHandler: () -> Unit,
    onEditHandler: () -> Unit,
    onDeleteHandler: () -> Unit
) {
    TopAppBar(
        title = { Text(text = "$numSelectedItems selected") },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = onDismissHandler) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            if(numSelectedItems == 1) {
                IconButton(onClick = {
                    onEditHandler()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit",
                    )
                }
            }
            IconButton(onClick = {
                onDeleteHandler()
            }) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                )
            }
        }
    )
}
