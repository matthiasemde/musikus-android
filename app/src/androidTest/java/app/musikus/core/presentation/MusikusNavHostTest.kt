/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.presentation.theme.MusikusTheme
import app.musikus.settings.domain.ColorSchemeSelections
import app.musikus.settings.domain.ThemeSelections
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class MusikusNavHostTest {

    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val mainViewModel: MainViewModel = hiltViewModel()

            val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val eventHandler = mainViewModel::onUiEvent

            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            MusikusTheme(
                theme = ThemeSelections.DAY,
                colorScheme = ColorSchemeSelections.MUSIKUS,
            ) {
                MusikusNavHost(
                    navController = navController,
                    mainUiState = mainUiState,
                    mainEventHandler = eventHandler,
                    bottomBarHeight = 0.dp,
                    timeProvider = fakeTimeProvider,
                )
            }
        }
    }

    @Test
    fun testStartDestination() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.Home::class.java)
        require(screen is Screen.Home)

        assertThat(screen.tab).isEqualTo(HomeTab.Sessions)
        composeRule.onNodeWithText("Sessions").assertIsDisplayed()
    }

    @Test
    fun testNavigationToGoals() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.Home(HomeTab.Goals))
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.Home::class.java)
        require(screen is Screen.Home)

        assertThat(screen.tab).isEqualTo(HomeTab.Goals)
        composeRule.onNodeWithText("Goals").assertIsDisplayed()
    }

    @Test
    fun testNavigationToStatistics() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.Home(HomeTab.Statistics))
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.Home::class.java)
        require(screen is Screen.Home)
        assertThat(screen.tab).isEqualTo(HomeTab.Statistics)
        composeRule.onNodeWithText("Statistics").assertIsDisplayed()
    }

    @Test
    fun testNavigationToLibrary() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.Home(HomeTab.Library))
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.Home::class.java)
        require(screen is Screen.Home)

        assertThat(screen.tab).isEqualTo(HomeTab.Library)
        composeRule.onNodeWithText("Library").assertIsDisplayed()
    }

    @Test
    fun testNavigationToActiveSession() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.ActiveSession())
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.ActiveSession::class.java)
        composeRule.onNodeWithText("Practice Time").assertIsDisplayed()
    }

    @Test
    fun testNavigationToSessionStatistics() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.SessionStatistics)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.SessionStatistics::class.java)
        composeRule.onNodeWithText("Session History").assertIsDisplayed()
    }

    @Test
    fun testNavigationToGoalStatistics() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.GoalStatistics)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.GoalStatistics::class.java)
        composeRule.onNodeWithText("Goal History").assertIsDisplayed()
    }

    @Test
    fun testNavigationToSettings() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.Settings)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.Settings::class.java)
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun testNavigationToAbout() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.SettingsOption.About)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.SettingsOption.About::class.java)
        composeRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun testNavigationToHelp() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.SettingsOption.Help)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.SettingsOption.Help::class.java)
        composeRule.onNodeWithText("Help").assertIsDisplayed()
    }

    @Test
    fun testNavigationToBackup() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.SettingsOption.Backup)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.SettingsOption.Backup::class.java)
        composeRule.onNodeWithText("Backup and restore").assertIsDisplayed()
    }

    @Test
    fun testNavigationToExport() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.SettingsOption.Export)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.SettingsOption.Export::class.java)
        composeRule.onNodeWithText("Export session data").assertIsDisplayed()
    }

    @Test
    fun testNavigationToDonate() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.SettingsOption.Donate)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.SettingsOption.Donate::class.java)
        composeRule.onNodeWithText("Support us!").assertIsDisplayed()
    }

    @Test
    fun testNavigationToAppearance() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.SettingsOption.Appearance)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.SettingsOption.Appearance::class.java)
        composeRule.onNodeWithText("Appearance").assertIsDisplayed()
    }

    @Test
    fun testNavigationToLicense() = runTest {
        composeRule.awaitIdle() // ensures that navController is initialized

        composeRule.runOnUiThread {
            navController.navigate(Screen.License)
        }

        val screen = navController.currentBackStackEntry?.toScreen()

        assertThat(screen).isInstanceOf(Screen.License::class.java)
        composeRule.onNodeWithText("Licenses").assertIsDisplayed()
    }
}
