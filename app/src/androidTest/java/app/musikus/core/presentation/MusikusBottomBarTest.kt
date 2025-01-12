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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import app.musikus.core.domain.FakeTimeProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class MusikusBottomBarTest {

    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    lateinit var navController: NavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val mainViewModel: MainViewModel = hiltViewModel()

            val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val eventHandler = mainViewModel::onUiEvent

            navController = mockk<NavHostController>(relaxed = true)

            MusikusBottomBar(
                mainUiState = mainUiState,
                mainEventHandler = eventHandler,
                currentTab = HomeTab.Sessions,
                onTabSelected = { selectedTab ->
                    navController.navigate(Screen.Home(selectedTab))
                },
            )
        }
    }

    @Test
    fun navigateToSessions() = runTest {
        composeRule.onNodeWithText("Sessions").performClick()

        // Since we are already on the sessions tab, we should not navigate
        verify(exactly = 0) {
            navController.navigate<Any>(any())
        }
    }

    @Test
    fun navigateToGoals() = runTest {
        composeRule.onNodeWithText("Goals").performClick()

        verify(exactly = 1) {
            navController.navigate(Screen.Home(HomeTab.Goals))
        }
    }

    @Test
    fun navigateToStatistics() = runTest {
        composeRule.onNodeWithText("Statistics").performClick()

        verify(exactly = 1) {
            navController.navigate(Screen.Home(HomeTab.Statistics))
        }
    }

    @Test
    fun navigateToLibrary() = runTest {
        composeRule.onNodeWithText("Library").performClick()

        verify(exactly = 1) {
            navController.navigate(Screen.Home(HomeTab.Library))
        }
    }
}
