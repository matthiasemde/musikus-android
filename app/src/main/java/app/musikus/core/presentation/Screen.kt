/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.toRoute
import app.musikus.R
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(
    val route: String
) {

    @Serializable
    class ActiveSession : Screen("activeSession")

    @Serializable
    data class Home (
        val tab: HomeTab = HomeTab.Sessions
    ): Screen("home")

    @Serializable
    class Settings : Screen("settings")

    @Serializable
    sealed class SettingsOption(val subRoute: String) : Screen("settings/$subRoute") {
        @Serializable
        class About : SettingsOption("about")
        @Serializable
        class Help : SettingsOption("help")
        @Serializable
        class Backup : SettingsOption("backup")
        @Serializable
        class Export : SettingsOption("export")
        @Serializable
        class Donate : SettingsOption("donate")
        @Serializable
        class Appearance : SettingsOption("appearance")

        companion object {
            val all by lazy {
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

    @Serializable
    class EditSession : Screen("editSession")

    @Serializable
    class SessionStatistics : Screen("sessionStatistics")

    @Serializable
    class GoalStatistics : Screen("goalStatistics")

    @Serializable
    class License : Screen("license")
}

fun NavBackStackEntry.toScreen() : Screen {
    val route = arguments?.getString("route")
        ?: throw IllegalArgumentException("Route argument missing from $this")

    return when (route) {
        Screen.ActiveSession().route -> toRoute<Screen.ActiveSession>()
        Screen.Home().route -> toRoute<Screen.Home>()
        Screen.Settings().route -> toRoute<Screen.Settings>()
        Screen.SettingsOption.About().route -> toRoute<Screen.SettingsOption.About>()
        Screen.SettingsOption.Help().route -> toRoute<Screen.SettingsOption.Help>()
        Screen.SettingsOption.Backup().route -> toRoute<Screen.SettingsOption.Backup>()
        Screen.SettingsOption.Export().route -> toRoute<Screen.SettingsOption.Export>()
        Screen.SettingsOption.Donate().route -> toRoute<Screen.SettingsOption.Donate>()
        Screen.SettingsOption.Appearance().route -> toRoute<Screen.SettingsOption.Appearance>()
        Screen.EditSession().route -> toRoute<Screen.EditSession>()
        Screen.SessionStatistics().route -> toRoute<Screen.SessionStatistics>()
        Screen.GoalStatistics().route -> toRoute<Screen.GoalStatistics>()
        Screen.License().route -> toRoute<Screen.License>()
        else -> throw IllegalArgumentException("Unknown route: $route")
    }
}

@Serializable
sealed class HomeTab {
    data object Sessions : HomeTab()
    data object Goals : HomeTab()
    data object Statistics : HomeTab()
    data object Library : HomeTab()

    companion object {
        val all by lazy { listOf(Sessions, Goals, Statistics, Library) }
        val default = Sessions
    }
}

val HomeTabNavType = object : NavType<HomeTab>(isNullableAllowed = false) {
    override fun get(
        bundle: Bundle,
        key: String
    ): HomeTab? {
        return bundle.getString(key)?.let {
             parseValue(it)
        }
    }

    override fun put(
        bundle: Bundle,
        key: String,
        value: HomeTab
    ) {
        bundle.putString(key, serializeAsValue(value))
    }

    override fun parseValue(value: String): HomeTab {
        return Uri.decode(value).let {
            HomeTab.all.first { tab -> tab.toString() == it }
        }
    }

    override fun serializeAsValue(value: HomeTab): String {
        return Uri.encode(value.toString())
    }
}

data class DisplayData(
    val title: UiText,
    val icon: UiIcon,
    @DrawableRes val animatedIcon: Int? = null
)

fun HomeTab.getDisplayData(): DisplayData {
    return when(this) {
        is HomeTab.Sessions -> DisplayData(
            title = UiText.StringResource(R.string.components_bottom_bar_items_sessions),
            icon = UiIcon.IconResource(R.drawable.ic_sessions),
            animatedIcon = R.drawable.avd_sessions,
        )

        is HomeTab.Goals -> DisplayData(
            title = UiText.StringResource(R.string.components_bottom_bar_items_goals),
            icon = UiIcon.IconResource(R.drawable.ic_goals),
            animatedIcon = R.drawable.avd_goals
        )

        is HomeTab.Statistics -> DisplayData(
            title = UiText.StringResource(R.string.components_bottom_bar_items_statistics),
            icon = UiIcon.IconResource(R.drawable.ic_bar_chart),
            animatedIcon = R.drawable.avd_bar_chart
        )

        is HomeTab.Library -> DisplayData(
            title = UiText.StringResource(R.string.components_bottom_bar_items_library),
            icon = UiIcon.IconResource(R.drawable.ic_library),
            animatedIcon = R.drawable.avd_library
        )
    }
}

fun Screen.SettingsOption.getDisplayData(): DisplayData {
    return when(this) {
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
    }
}