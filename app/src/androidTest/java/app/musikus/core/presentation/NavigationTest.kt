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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class NavigationTest {

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

            MusikusNavHost(
                navController = navController,
                mainUiState = mainUiState,
                mainEventHandler = eventHandler,
                bottomBarHeight = 0.dp,
                timeProvider = fakeTimeProvider,
            )
        }
    }

    @Test
    fun verifyStartDestination() {
        composeRule.onNodeWithText("Sessions").assertIsDisplayed()
    }
}