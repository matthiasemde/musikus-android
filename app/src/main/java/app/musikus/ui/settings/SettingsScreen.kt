/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.musikus.ui.Screen
import app.musikus.ui.navigateTo
import app.musikus.ui.settings.about.AboutScreen
import app.musikus.ui.theme.spacing

fun NavGraphBuilder.addSettingsNavigationGraph(navController: NavController) {
    composable(Screen.Settings.route) {
        SettingsScreen(
            navigateUp = { navController.navigateUp() },
            navigateTo = navController::navigateTo
        )
    }
    composable(Screen.SettingsOptions.About.route) {
        AboutScreen(
            navigateUp = { navController.navigateUp() }
        )
    }
    composable(Screen.SettingsOptions.Appearance.route) {

    }
    composable(Screen.SettingsOptions.Backup.route) {

    }
    composable(Screen.SettingsOptions.Donate.route) {

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigateUp: () -> Unit,
    navigateTo: (Screen) -> Unit,
) {
    val settingsItems = listOf(
        listOf(Screen.SettingsOptions.Donate),
        listOf(
            Screen.SettingsOptions.Appearance,
            Screen.SettingsOptions.Backup
        ),
        listOf(Screen.SettingsOptions.About),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            for(group in settingsItems) {
                for (settingsItem in group) {
                    if (settingsItem.displayData == null) {
                        throw Exception("No display data for $settingsItem")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigateTo(settingsItem) }
                            .padding(
                                horizontal = MaterialTheme.spacing.extraLarge,
                                vertical = MaterialTheme.spacing.medium
                            )
                    ) {
                        Icon(
                            imageVector = settingsItem.displayData.icon.asIcon(),
                            contentDescription = settingsItem.displayData.title.asString()
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                        Text(text = settingsItem.displayData.title.asString())
                    }
                }
                if(group != settingsItems.last()) {
                    HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
                }
            }
        }
    }
}

