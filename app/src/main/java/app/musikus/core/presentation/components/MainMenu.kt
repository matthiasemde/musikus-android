/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.musikus.R
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.theme.spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainMenu(
    navigateTo: (Screen) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Text(
        modifier = Modifier.padding(MaterialTheme.spacing.medium),
        text = stringResource(R.string.core_app_name)
    )
    NavigationDrawerItem(
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding),
        icon = {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.components_main_menu)
            )
        },
        label = { Text(text = stringResource(R.string.components_main_menu)) },
        selected = false,
        onClick = {
            scope.launch {
                onDismiss()
                delay(200.milliseconds)
                navigateTo(Screen.Settings)
            }
        }
    )
}
