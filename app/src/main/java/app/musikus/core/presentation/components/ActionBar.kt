/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.musikus.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBar(
    numSelectedItems: Int,
    uniqueActions: @Composable () -> Unit = {},
    editActionEnabled: () -> Boolean = { numSelectedItems == 1 },
    onDismissHandler: () -> Unit,
    onEditHandler: () -> Unit,
    onDeleteHandler: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.components_action_bar_title, numSelectedItems)) },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        navigationIcon = {
            IconButton(onClick = onDismissHandler) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.core_back_button_description)
                )
            }
        },
        actions = {
            uniqueActions()
            if(editActionEnabled()) {
                IconButton(onClick = onEditHandler) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(id = R.string.core_edit_button_description)
                    )
                }
            }
            IconButton(onClick = onDeleteHandler) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(id = R.string.core_delete_button_description)
                )
            }
        }
    )
}