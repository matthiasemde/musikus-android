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
import androidx.compose.material.icons.outlined.Settings
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.toRoute
import app.musikus.R
import app.musikus.activesession.presentation.ActiveSessionActions
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import kotlinx.serialization.Serializable

/**
 * Sealed class representing different screens in the application.
 */
@Serializable
sealed class Screen {

    @Serializable
    data class ActiveSession(
        val action: ActiveSessionActions? = null
    ) : Screen()

    @Serializable
    data class Home(
        val tab: HomeTab = HomeTab.Sessions
    ) : Screen()


    @Serializable
    sealed class MainMenuEntry : Screen() {
        @Serializable
        data object Settings : MainMenuEntry()

        @Serializable
        data object About : MainMenuEntry()

        @Serializable
        data object Help : MainMenuEntry()

        @Serializable
        data object Donate : MainMenuEntry()

        companion object {
            val all by lazy {
                listOf(
                    Settings,
                    About,
                    Help,
                    Donate
                )
            }
        }
    }

    @Serializable
    sealed class SettingsOption : Screen() {
        @Serializable
        data object Backup : SettingsOption()

        @Serializable
        data object Export : SettingsOption()

        @Serializable
        data object Appearance : SettingsOption()

        companion object {
            val all by lazy {
                listOf(
                    Backup,
                    Export,
                    Appearance
                )
            }
        }
    }

    @Serializable
    data object SessionStatistics : Screen()

    @Serializable
    data object GoalStatistics : Screen()

    @Serializable
    data object License : Screen()

    companion object {
        val all by lazy {
            listOf(
                ActiveSession(),
                Home(),
                SessionStatistics,
                GoalStatistics,
                License
            ) + SettingsOption.all + MainMenuEntry.all
        }
    }
}

val Screen.route: String?
    get() = this.javaClass.canonicalName

/**
 * Converts a [NavBackStackEntry] to a [Screen].
 * This function is necessary because there is no direct way of getting a typed [Screen] object from
 * the [NavBackStackEntry] outside the NavHost. It does so by parsing the route of the destination
 * and comparing it to the canonical class name of the [Screen] object.
 */
fun NavBackStackEntry.toScreen(): Screen {
    val route = destination.route?.split(Regex("[^a-zA-Z0-9.]"))?.first()
        ?: throw IllegalArgumentException("Route argument missing from $destination")

    return when (route) {
        Screen.ActiveSession().route -> toRoute<Screen.ActiveSession>()
        Screen.Home().route -> toRoute<Screen.Home>()
        Screen.MainMenuEntry.Settings.route -> toRoute<Screen.MainMenuEntry.Settings>()
        Screen.MainMenuEntry.About.route -> toRoute<Screen.MainMenuEntry.About>()
        Screen.MainMenuEntry.Help.route -> toRoute<Screen.MainMenuEntry.Help>()
        Screen.MainMenuEntry.Donate.route -> toRoute<Screen.MainMenuEntry.Donate>()
        Screen.SettingsOption.Backup.route -> toRoute<Screen.SettingsOption.Backup>()
        Screen.SettingsOption.Export.route -> toRoute<Screen.SettingsOption.Export>()
        Screen.SettingsOption.Appearance.route -> toRoute<Screen.SettingsOption.Appearance>()
        Screen.SessionStatistics.route -> toRoute<Screen.SessionStatistics>()
        Screen.GoalStatistics.route -> toRoute<Screen.GoalStatistics>()
        Screen.License.route -> toRoute<Screen.License>()
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
        val default by lazy { Sessions } // has to be lazy due to some testing peculiarities
    }
}

/**
 * Custom [NavType] for [HomeTab].
 * This is necessary for type-safe navigation because [HomeTab] is a sealed class
 * and cannot be directly serialized or deserialized.
 */
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

/**
 * Data class representing display data for a tab or screen.
 * @property title The title of the tab or screen.
 * @property icon The icon of the tab or screen.
 * @property animatedIcon The animated icon of the tab or screen.
 */
data class DisplayData(
    val title: UiText,
    val icon: UiIcon,
    @DrawableRes val animatedIcon: Int? = null
)

/**
 * Extension function to get the display data for a [HomeTab].
 * @return The display data for the tab.
 */
fun HomeTab.getDisplayData(): DisplayData {
    return when (this) {
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

/**
 * Extension function to get the display data for a [Screen.MainMenuEntry].
 * @return The display data for the main menu entry.
 */
fun Screen.MainMenuEntry.getDisplayData(): DisplayData {
    return when (this) {
        is Screen.MainMenuEntry.Settings -> DisplayData(
            title = UiText.StringResource(R.string.components_main_menu_item_settings),
            icon = UiIcon.DynamicIcon(Icons.Outlined.Settings),
        )
        is Screen.MainMenuEntry.About -> DisplayData(
            title = UiText.StringResource(R.string.components_main_menu_item_about),
            icon = UiIcon.DynamicIcon(Icons.Outlined.Info),
        )
        is Screen.MainMenuEntry.Help -> DisplayData(
            title = UiText.StringResource(R.string.components_main_menu_item_help),
            icon = UiIcon.DynamicIcon(Icons.AutoMirrored.Outlined.Help),
        )
        is Screen.MainMenuEntry.Donate -> DisplayData(
            title = UiText.StringResource(R.string.components_main_menu_item_donate),
            icon = UiIcon.DynamicIcon(Icons.Outlined.Favorite),
        )
    }
}

/**
 * Extension function to get the display data for a [Screen.SettingsOption].
 * @return The display data for the settings option.
 */
fun Screen.SettingsOption.getDisplayData(): DisplayData {
    return when(this) {
        is Screen.SettingsOption.Backup -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_backup),
            icon = UiIcon.DynamicIcon(Icons.Outlined.CloudUpload),
        )
        is Screen.SettingsOption.Export -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_export),
            icon = UiIcon.IconResource(R.drawable.ic_export),
        )
        is Screen.SettingsOption.Appearance -> DisplayData(
            title = UiText.StringResource(R.string.settings_items_appearance),
            icon = UiIcon.IconResource(R.drawable.ic_appearance),
        )
    }
}
