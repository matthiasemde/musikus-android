/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.core.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.musikus.R
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.getDisplayData
import app.musikus.core.presentation.theme.spacing
import app.musikus.menu.presentation.about.AboutScreen
import app.musikus.menu.presentation.about.LicensesScreen
import app.musikus.menu.presentation.donate.DonateScreen
import app.musikus.menu.presentation.help.HelpScreen
import app.musikus.menu.presentation.settings.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainMenu(
    navigateTo: (Screen) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val menuEntryGroups = listOf(
        listOf(Screen.MainMenuEntry.Donate),
        listOf(
            Screen.MainMenuEntry.Settings,
            Screen.MainMenuEntry.Help,
            Screen.MainMenuEntry.About
        )
    )

    Column {
        Text(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            text = stringResource(R.string.core_app_name),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.weight(1f))

        // Link to Discord
        NavigationDrawerItem(
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            icon = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_discord),
                    contentDescription = stringResource(R.string.menu_connect_title)
                )
            },
            label = { Text(text = stringResource(R.string.menu_connect_title)) },
            selected = false,
            onClick = {
                val openUrlIntent = Intent(Intent.ACTION_VIEW)
                openUrlIntent.data = Uri.parse(context.getString(R.string.menu_connect_discord_url))
                startActivity(context, openUrlIntent, null)
            }
        )

        // Link to GitHub
        NavigationDrawerItem(
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            icon = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_github),
                    contentDescription = stringResource(R.string.menu_contribute_title)
                )
            },
            label = { Text(text = stringResource(R.string.menu_contribute_title)) },
            selected = false,
            onClick = {
                val openUrlIntent = Intent(Intent.ACTION_VIEW)
                openUrlIntent.data = Uri.parse(context.getString(R.string.menu_contribute_github_url))
                startActivity(context, openUrlIntent, null)
            }
        )

        menuEntryGroups.forEach { group ->
            group.forEach { entry ->
                val displayData = entry.getDisplayData()
                NavigationDrawerItem(
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding),
                    icon = {
                        Icon(
                            displayData.icon.asIcon(),
                            contentDescription = displayData.title.asString()
                        )
                    },
                    label = { Text(text = displayData.title.asString()) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            onDismiss()
                            delay(200.milliseconds)
                            navigateTo(entry)
                        }
                    }
                )
            }

            if (group != menuEntryGroups.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }
}

fun NavGraphBuilder.addMainMenuNavigationGraph(navController: NavController) {
    composable<Screen.MainMenuEntry.Settings> {
        SettingsScreen(
            navigateUp = { navController.navigateUp() },
            navigateTo = { navController.navigate(it) }
        )
    }
    composable<Screen.MainMenuEntry.Donate> {
        DonateScreen(navigateUp = { navController.navigateUp() })
    }
    composable<Screen.MainMenuEntry.Help> {
        HelpScreen(navigateUp = { navController.navigateUp() })
    }
    composable<Screen.MainMenuEntry.About> {
        AboutScreen(
            navigateUp = { navController.navigateUp() },
            navigateTo = { navController.navigate(it) }
        )
    }
    composable<Screen.License> {
        LicensesScreen(navigateUp = { navController.navigateUp() })
    }
}
