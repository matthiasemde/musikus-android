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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.core.domain.FakeTimeProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
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

    lateinit var onTabSelectedMock: (HomeTab) -> Unit

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val mainViewModel: MainViewModel = hiltViewModel()

            val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val eventHandler = mainViewModel::onUiEvent

            var currentTab: HomeTab by remember { mutableStateOf(HomeTab.Sessions) }

            onTabSelectedMock = mockk()
            every { onTabSelectedMock(any()) } answers { currentTab = firstArg<HomeTab>() }

            MusikusBottomBar(
                mainUiState = mainUiState,
                mainEventHandler = eventHandler,
                currentTab = currentTab,
                onTabSelected = onTabSelectedMock,
            )
        }
    }

    @Test
    fun navigateToSessions() = runTest {
        composeRule.onNodeWithText("Sessions").performClick()

        // Since we are already on the sessions tab, we should not navigate
        verify(exactly = 0) {
            onTabSelectedMock(any())
        }
    }

    @Test
    fun navigateToGoals() = runTest {
        composeRule.onNodeWithText("Goals").performClick()

        verify(exactly = 1) {
            onTabSelectedMock(HomeTab.Goals)
        }

        composeRule.onNodeWithText("Goals").assertIsSelected()
    }

    @Test
    fun navigateToStatistics() = runTest {
        composeRule.onNodeWithText("Statistics").performClick()

        verify(exactly = 1) {
            onTabSelectedMock(HomeTab.Statistics)
        }

        composeRule.onNodeWithText("Statistics").assertIsSelected()
    }

    @Test
    fun navigateToLibrary() = runTest {
        composeRule.onNodeWithText("Library").performClick()

        verify(exactly = 1) {
            onTabSelectedMock(HomeTab.Library)
        }

        composeRule.onNodeWithText("Library").assertIsSelected()
    }
}
