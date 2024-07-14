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

sealed class Screen(
    val route: String,
    open val displayData: DisplayData? = null,
) {

    data object Home : Screen(
        route = "home"
    )

    sealed class HomeTab(
        val subRoute: String,
        override val displayData: DisplayData
    ) : Screen("home/$subRoute") {
        data object Sessions : HomeTab(
            subRoute = "sessions",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.navigationSessionsTitle),
                icon = UiIcon.IconResource(R.drawable.ic_sessions),
                animatedIcon = R.drawable.avd_sessions,
            )
        )

        data object Goals : HomeTab(
            subRoute = "goals",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.navigationGoalsTitle),
                icon = UiIcon.IconResource(R.drawable.ic_goals),
                animatedIcon = R.drawable.avd_goals
            )
        )

        data object Statistics : HomeTab(
            subRoute = "statistics",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.navigationStatisticsTitle),
                icon = UiIcon.IconResource(R.drawable.ic_bar_chart),
                animatedIcon = R.drawable.avd_bar_chart
            )
        )

        data object Library : HomeTab(
            subRoute = "library",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.navigationLibraryTitle),
                icon = UiIcon.IconResource(R.drawable.ic_library),
                animatedIcon = R.drawable.avd_library
            )
        )
        companion object {
            val allTabs by lazy { listOf(Sessions, Goals, Statistics, Library) }
            val defaultTab = Sessions
        }
    }

    data object ActiveSession : Screen(
        route = "activeSession",
    )

    data object EditSession : Screen(
        route = "editSession/{sessionId}",
    )


    data object SessionStatistics : Screen(
        route = "sessionStatistics",
    )
    data object GoalStatistics : Screen(
        route = "goalStatistics",
    )

    data object Settings : Screen(
        route = "settings",
    )

    data object License : Screen(
        route = "settings/about/license"
    )

    sealed class SettingsOption(
        subRoute: String,
        override val displayData: DisplayData
    ) : Screen("settings/$subRoute") {
        data object About : SettingsOption(
            subRoute = "about",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.about_app_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.Info),
            )
        )

        data object Help : SettingsOption(
            subRoute = "help",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.help_title),
                icon = UiIcon.DynamicIcon(Icons.AutoMirrored.Outlined.Help),
            )
        )

        data object Backup : SettingsOption(
            subRoute = "backup",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.backup_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.CloudUpload),
            )
        )

        data object Export : SettingsOption(
            subRoute = "export",
            displayData = DisplayData(
                title = UiText.DynamicString("Export session data"),
                icon = UiIcon.IconResource(R.drawable.ic_export),
            )
        )

        data object Donate : SettingsOption(
            subRoute = "donate",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.donations_title),
                icon = UiIcon.DynamicIcon(Icons.Outlined.Favorite),
            )
        )

        data object Appearance : SettingsOption(
            subRoute = "appearance",
            displayData = DisplayData(
                title = UiText.StringResource(R.string.appearance_title),
                icon = UiIcon.IconResource(R.drawable.ic_appearance),
            )
        )

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


    data class DisplayData(
        val title: UiText,
        val icon: UiIcon,
        @DrawableRes val animatedIcon: Int? = null
    )
}