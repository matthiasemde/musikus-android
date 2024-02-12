/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import app.musikus.ui.components.TwoLiner
import app.musikus.ui.components.TwoLinerData
import app.musikus.ui.navigateTo
import app.musikus.ui.settings.about.AboutScreen
import app.musikus.ui.settings.appearance.AppearanceScreen
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
        AppearanceScreen(
            navigateUp = { navController.navigateUp() }
        )
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
        listOf(TwoLinerData(
            icon = Screen.SettingsOptions.Donate.displayData!!.icon,
            firstLine = Screen.SettingsOptions.Donate.displayData.title,
            onClick = { navigateTo(Screen.SettingsOptions.Donate )}
        )),
        listOf(
            TwoLinerData(
                icon = Screen.SettingsOptions.Appearance.displayData!!.icon,
                firstLine = Screen.SettingsOptions.Appearance.displayData.title,
                onClick = { navigateTo(Screen.SettingsOptions.Appearance) }
            ),
            TwoLinerData(
                icon = Screen.SettingsOptions.Backup.displayData!!.icon,
                firstLine = Screen.SettingsOptions.Backup.displayData.title,
                onClick = { navigateTo(Screen.SettingsOptions.Backup )}
            )
        ),
        listOf(TwoLinerData(
            icon = Screen.SettingsOptions.About.displayData!!.icon,
            firstLine = Screen.SettingsOptions.About.displayData.title,
            onClick = { navigateTo(Screen.SettingsOptions.About )}
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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
    }
}

