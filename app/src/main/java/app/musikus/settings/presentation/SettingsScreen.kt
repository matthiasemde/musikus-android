/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.settings.presentation

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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.musikus.R
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.components.TwoLiner
import app.musikus.core.presentation.components.TwoLinerData
import app.musikus.core.presentation.getDisplayData
import app.musikus.core.presentation.theme.spacing
import app.musikus.settings.presentation.about.AboutScreen
import app.musikus.settings.presentation.about.LicensesScreen
import app.musikus.settings.presentation.appearance.AppearanceScreen
import app.musikus.settings.presentation.backup.BackupScreen
import app.musikus.settings.presentation.donate.DonateScreen
import app.musikus.settings.presentation.export.ExportScreen
import app.musikus.settings.presentation.help.HelpScreen

fun NavGraphBuilder.addSettingsNavigationGraph(navController: NavController) {
    composable<Screen.Settings> {
        SettingsScreen(
            navigateUp = { navController.navigateUp() },
            navigateTo = { navController.navigate(it) }
        )
    }
    composable<Screen.SettingsOption.Donate> {
        DonateScreen(navigateUp = { navController.navigateUp() })
    }
    composable<Screen.SettingsOption.Appearance> {
        AppearanceScreen(navigateUp = { navController.navigateUp() })
    }
    composable<Screen.SettingsOption.Backup> {
        BackupScreen(navigateUp = { navController.navigateUp() })
    }
    composable<Screen.SettingsOption.Export> {
        ExportScreen(navigateUp = { navController.navigateUp() })
    }
    composable<Screen.SettingsOption.Help> {
        HelpScreen(navigateUp = { navController.navigateUp() })
    }
    composable<Screen.SettingsOption.About> {
        AboutScreen(
            navigateUp = { navController.navigateUp() },
            navigateTo = { navController.navigate(it) }
        )
    }
    composable<Screen.License> {
        LicensesScreen(navigateUp = { navController.navigateUp() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigateUp: () -> Unit,
    navigateTo: (Screen) -> Unit,
) {
    val settingsItems = listOf(
        listOf(
            TwoLinerData(
                icon = Screen.SettingsOption.Donate.getDisplayData().icon,
                firstLine = Screen.SettingsOption.Donate.getDisplayData().title,
                onClick = { navigateTo(Screen.SettingsOption.Donate) }
            )
        ),
        listOf(
            TwoLinerData(
                icon = Screen.SettingsOption.Appearance.getDisplayData().icon,
                firstLine = Screen.SettingsOption.Appearance.getDisplayData().title,
                onClick = { navigateTo(Screen.SettingsOption.Appearance) }
            ),
            TwoLinerData(
                icon = Screen.SettingsOption.Backup.getDisplayData().icon,
                firstLine = Screen.SettingsOption.Backup.getDisplayData().title,
                onClick = { navigateTo(Screen.SettingsOption.Backup) }
            ),
            TwoLinerData(
                icon = Screen.SettingsOption.Export.getDisplayData().icon,
                firstLine = Screen.SettingsOption.Export.getDisplayData().title,
                onClick = { navigateTo(Screen.SettingsOption.Export) }
            )
        ),
        listOf(
            TwoLinerData(
                icon = Screen.SettingsOption.Help.getDisplayData().icon,
                firstLine = Screen.SettingsOption.Help.getDisplayData().title,
                onClick = { navigateTo(Screen.SettingsOption.Help) }
            ),
            TwoLinerData(
                icon = Screen.SettingsOption.About.getDisplayData().icon,
                firstLine = Screen.SettingsOption.About.getDisplayData().title,
                onClick = { navigateTo(Screen.SettingsOption.About) }
            ),
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.components_top_bar_back_description),
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
                for (group in settingsItems) {
                    for (settingsItem in group) {
                        TwoLiner(data = settingsItem)
                    }
                    if (group != settingsItems.last()) {
                        HorizontalDivider(Modifier.padding(vertical = MaterialTheme.spacing.medium))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_footer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }
        }
    }
}
