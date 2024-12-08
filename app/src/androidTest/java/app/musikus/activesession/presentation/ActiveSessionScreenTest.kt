/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.activesession.presentation

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.musikus.core.data.Nullable
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.presentation.MainActivity
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.library.domain.usecase.LibraryUseCases
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class ActiveSessionScreenTest {
    @Inject lateinit var libraryUseCases: LibraryUseCases

    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()

        composeRule.activity.setContent {
            runBlocking {
                libraryUseCases.addFolder(
                    LibraryFolderCreationAttributes("TestFolder1")
                )
                libraryUseCases.addItem(
                    LibraryItemCreationAttributes(
                        name = "TestItem1",
                        colorIndex = 1,
                    )
                )
                libraryUseCases.addItem(
                    LibraryItemCreationAttributes(
                        name = "TestItem2",
                        colorIndex = 1,
                        libraryFolderId = Nullable(UUIDConverter.fromInt(1))
                    )
                )
            }

            ActiveSession(
                navigateUp = {}
            )
        }
    }

    @Test
    fun startSession_fabChanges() {
        composeRule.onNodeWithContentDescription("Start practicing").performClick()

        composeRule.onNodeWithText("TestItem1").performClick()
        composeRule.onNodeWithContentDescription("Next item").assertIsDisplayed()
    }

    @Test
    fun pauseAndResumeSession() {
        // Start session
        composeRule.onNodeWithContentDescription("Start practicing").performClick()
        composeRule.onNodeWithText("TestItem1").performClick()

        // Pause session after advancing time by 1 second
        // (a session with 0 seconds can not be paused)
        fakeTimeProvider.advanceTimeBy(1.seconds)
        composeRule.onNodeWithContentDescription("Pause").performClick()

        // Pause timer is displayed
        composeRule.onNodeWithText("Paused 00:00").assertIsDisplayed()

        fakeTimeProvider.advanceTimeBy(90.seconds)

        runBlocking {
            delay(100)
        }

        // Pause timer shows correct time
        composeRule.onNodeWithText("Paused 01:30")
            .assertIsDisplayed()
            .performClick() // Resume session

        // Pause timer is hidden
        composeRule.onNodeWithText("Paused", substring = true).assertIsNotDisplayed()
    }
}
