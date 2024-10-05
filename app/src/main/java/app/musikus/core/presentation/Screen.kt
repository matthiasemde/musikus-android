/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import app.musikus.R
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(
    val route: String,
) {

    data object Home : Screen(
        route = "home"
    )

    @Serializable
    sealed class HomeTab(
        val subRoute: String,
    ) : Screen("home/$subRoute") {
        data object Sessions : HomeTab("sessions")
        data object Goals : HomeTab("goals")
        data object Statistics : HomeTab("statistics")
        data object Library : HomeTab("library")

        companion object {
            val allTabs by lazy { listOf(Sessions, Goals, Statistics, Library) }
            val defaultTab = Sessions
        }
    }

    data object ActiveSession : Screen("activeSession")

    data object EditSession : Screen("editSession/{sessionId}")

    data object SessionStatistics : Screen("sessionStatistics")
    data object GoalStatistics : Screen("goalStatistics")

    data object Settings : Screen("settings")

    data object License : Screen("settings/about/license")

    sealed class SettingsOption(
        subRoute: String,
    ) : Screen("settings/$subRoute") {
        data object About : SettingsOption("about")
        data object Help : SettingsOption("help")
        data object Backup : SettingsOption("backup")
        data object Export : SettingsOption("export")
        data object Donate : SettingsOption("donate")
        data object Appearance : SettingsOption("appearance")

        companion object {
            val allSettings by lazy {
                listOf(
                    About,
                    Help,
                    Backup,
                    Export,
                    Donate,
                    Appearance
                )
            }
        }
    }
}

data class DisplayData(
    val title: UiText,
    val icon: UiIcon,
    @DrawableRes val animatedIcon: Int? = null
)

fun Screen.getDisplayData(): DisplayData? {
    return when(this) {
        is Screen.HomeTab.Sessions -> DisplayData(
            title = UiText.StringResource(R.string.components_bottom_bar_items_sessions),
            icon = UiIcon.IconResource(R.drawable.ic_sessions),
            animatedIcon = R.drawable.avd_sessions,
        )
        is Screen.HomeTab.Goals -> DisplayData(
            title = UiText.StringResource(R.string.components_bottom_bar_items_goals),
            icon = UiIcon.IconResource(R.drawable.ic_goals),
            animatedIcon = R.drawable.avd_goals
        )
        is Screen.HomeTab.Statistics -> DisplayData(
            title = UiText.StringResource(R.string.components_bottom_bar_items_statistics),
            icon = UiIcon.IconResource(R.drawable.ic_bar_chart),
            animatedIcon = R.drawable.avd_bar_chart
        )
        is Screen.HomeTab.Library -> DisplayData(
            title = UiText.StringResource(R.string.components_bottom_bar_items_library),
            icon = UiIcon.IconResource(R.drawable.ic_library),
            animatedIcon = R.drawable.avd_library
        )
        is Screen.SettingsOption.About -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_about),
            icon = UiIcon.DynamicIcon(Icons.Outlined.Info),
        )
        is Screen.SettingsOption.Help -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_help),
            icon = UiIcon.DynamicIcon(Icons.AutoMirrored.Outlined.Help),
        )
        is Screen.SettingsOption.Backup -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_backup),
            icon = UiIcon.DynamicIcon(Icons.Outlined.CloudUpload),
        )
        is Screen.SettingsOption.Export -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_export),
            icon = UiIcon.IconResource(R.drawable.ic_export),
        )
        is Screen.SettingsOption.Donate -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_donate),
            icon = UiIcon.DynamicIcon(Icons.Outlined.Favorite),
        )
        is Screen.SettingsOption.Appearance -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_appearance),
            icon = UiIcon.IconResource(R.drawable.ic_appearance),
        )
        else -> null
    }
}