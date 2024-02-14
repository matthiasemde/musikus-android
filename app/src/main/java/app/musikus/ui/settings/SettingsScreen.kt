/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import app.musikus.ui.components.TwoLiner
import app.musikus.ui.components.TwoLinerData
import app.musikus.ui.navigateTo
import app.musikus.ui.settings.about.AboutScreen
import app.musikus.ui.settings.appearance.AppearanceScreen
import app.musikus.ui.settings.backup.BackupScreen
import app.musikus.ui.settings.donate.DonateScreen
import app.musikus.ui.theme.spacing

fun NavGraphBuilder.addSettingsNavigationGraph(navController: NavController) {
    composable(Screen.Settings.route) {
        SettingsScreen(
            navigateUp = { navController.navigateUp() },
            navigateTo = navController::navigateTo
        )
    }
    composable(Screen.SettingsOption.About.route) {
        AboutScreen(navigateUp = { navController.navigateUp() })
    }
    composable(Screen.SettingsOption.Appearance.route) {
        AppearanceScreen(navigateUp = { navController.navigateUp() })
    }
    composable(Screen.SettingsOption.Backup.route) {
        BackupScreen(navigateUp = { navController.navigateUp() })
    }
    composable(Screen.SettingsOption.Donate.route) {
        DonateScreen(navigateUp = { navController.navigateUp() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigateUp: () -> Unit,
    navigateTo: (Screen) -> Unit,
) {
    val settingsItems = listOf(
        listOf(TwoLinerData(
            icon = Screen.SettingsOption.Donate.displayData.icon,
            firstLine = Screen.SettingsOption.Donate.displayData.title,
            onClick = { navigateTo(Screen.SettingsOption.Donate )}
        )),
        listOf(
            TwoLinerData(
                icon = Screen.SettingsOption.Appearance.displayData.icon,
                firstLine = Screen.SettingsOption.Appearance.displayData.title,
                onClick = { navigateTo(Screen.SettingsOption.Appearance) }
            ),
            TwoLinerData(
                icon = Screen.SettingsOption.Backup.displayData.icon,
                firstLine = Screen.SettingsOption.Backup.displayData.title,
                onClick = { navigateTo(Screen.SettingsOption.Backup) }
            )
        ),
        listOf(TwoLinerData(
            icon = Screen.SettingsOption.About.displayData.icon,
            firstLine = Screen.SettingsOption.About.displayData.title,
            onClick = { navigateTo(Screen.SettingsOption.About )}
        )),
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
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                for(group in settingsItems) {
                    for (settingsItem in group) {
                        TwoLiner(data = settingsItem)
                    }
                    if(group != settingsItems.last()) {
                        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Made with ❤️ in Munich",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }
        }
    }
}

