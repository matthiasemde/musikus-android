/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.musikus.R
import app.musikus.core.presentation.utils.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusikusTopBar(
    isTopLevel: Boolean,
    title: UiText,
    scrollBehavior: TopAppBarScrollBehavior,
    Actions: @Composable () -> Unit = {},
    OverflowActions: (@Composable () -> Unit)? = null, // Should be a list of DropdownMenuItem()
    navigateUp: () -> Unit,
    openMainMenu: () -> Unit,
) {
    LargeTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(text = title.asString()) },
        navigationIcon = {
            if (isTopLevel) {
                IconButton(onClick = { openMainMenu() }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(
                            id = R.string.components_top_bar_menu_description
                        )
                    )
                }
            } else {
                IconButton(onClick = { navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(
                            id = R.string.components_top_bar_back_description
                        )
                    )
                }
            }
        },
        actions = {
            // Screen specific actions
            Actions()

            /*
             * Optional screen specific overflow menu
             */
            if (OverflowActions == null) {
                return@LargeTopAppBar
            }

            var showOverflowMenu by remember { mutableStateOf(false) }

            IconButton(onClick = { showOverflowMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.core_kebab_menu_description)
                )
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                ) {
                    OverflowActions()
                }
            }
        }
    )
}
