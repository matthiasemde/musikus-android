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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.navigation.NavHostController
import app.musikus.core.data.Nullable
import app.musikus.core.data.SectionWithLibraryItem
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.domain.plus
import app.musikus.core.presentation.MainActivity
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.MainViewModel
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.sessions.data.daos.Section
import app.musikus.sessions.data.daos.Session
import app.musikus.sessions.domain.usecase.SessionsUseCases
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class ActiveSessionScreenTest {
    @Inject lateinit var libraryUseCases: LibraryUseCases

    @Inject lateinit var sessionsUseCases: SessionsUseCases

    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    lateinit var navController: NavHostController
    lateinit var mainViewModel: MainViewModel

    @Before
    fun setUp() {
        hiltRule.inject()

        navController = mockk<NavHostController>(relaxed = true)
        mainViewModel = mockk<MainViewModel>(relaxed = true)

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
                    colorIndex = 2,
                )
            )
            libraryUseCases.addItem(
                LibraryItemCreationAttributes(
                    name = "TestItem3",
                    colorIndex = 1,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1))
                )
            )
        }

        composeRule.activity.setContent {
            ActiveSession(
                navigateUp = navController::navigateUp,
                mainEventHandler = mainViewModel::onUiEvent,
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

        // Pause session
        composeRule.onNodeWithContentDescription("Pause").performClick()

        // Pause timer is displayed
        composeRule.onNodeWithText("Paused 00:00").assertIsDisplayed()

        fakeTimeProvider.advanceTimeBy(90.seconds)

        // Pause timer shows correct time
        composeRule.onNodeWithText("Paused 01:30")
            .assertIsDisplayed()
            .performClick() // Resume session

        // Pause timer is hidden
        composeRule.onNodeWithText("Paused", substring = true).assertIsNotDisplayed()
    }

    @Test
    fun selectItemFromFolder() {
        // Open item selector
        composeRule.onNodeWithContentDescription("Start practicing").performClick()

        // Select folder
        composeRule.onNodeWithText("TestFolder1").performClick()

        // Select item
        composeRule.onNodeWithText("TestItem3").performClick()

        // Item is selected
        composeRule.onNodeWithText("TestItem3").assertIsDisplayed()
    }

    @Test
    fun practiceMultipleItemsInARow() {
        // Open item selector
        composeRule.onNodeWithContentDescription("Start practicing").performClick()

        // Select item
        composeRule.onNodeWithText("TestItem1").performClick()

        // Item is selected
        composeRule.onNodeWithText("TestItem1").assertIsDisplayed()

        // Open item selector again
        composeRule.onNodeWithContentDescription("Next item").performClick()

        // Select next item
        composeRule.onNodeWithText("TestItem2").performClick()

        // Item is selected
        composeRule.onNodeWithText("TestItem2").assertIsDisplayed()
    }

    @Test
    fun discardSession() = runTest {
        // Start session
        composeRule.onNodeWithContentDescription("Start practicing").performClick()
        composeRule.onNodeWithText("TestItem1").performClick()

        // Discard session
        composeRule.onNodeWithContentDescription("Discard").performClick()

        // Confirm discard
        composeRule.onNodeWithText("Discard session?", substring = true).performClick()

        // Navigate up is called
        verify(exactly = 1) {
            navController.navigateUp()
        }

        // Sessions are still empty
        val sessions = sessionsUseCases.getAll().first()
        assertThat(sessions).isEmpty()
    }

    @Test
    fun deleteSection() = runTest {
        // Start session
        composeRule.onNodeWithContentDescription("Start practicing").performClick()
        composeRule.onNodeWithText("TestItem1").performClick()

        // Advance time
        fakeTimeProvider.advanceTimeBy(3.minutes)

        // Start next section
        composeRule.onNodeWithContentDescription("Next item").performClick()
        composeRule.onNodeWithText("TestItem2").performClick()

        // Delete previous section
        composeRule.onNodeWithText("TestItem1").performTouchInput { swipeLeft() }

        composeRule.awaitIdle()

        // Assert showSnackbar is called
        val uiEventSlot = slot<MainUiEvent>()

        verify(exactly = 1) {
            mainViewModel.onUiEvent(capture(uiEventSlot))
        }

        val uiEvent = uiEventSlot.captured
        check(uiEvent is MainUiEvent.ShowSnackbar)
        assertThat(uiEvent.message).isEqualTo("Section deleted")
        assertThat(uiEvent.onUndo).isNotNull()

        // Assert section is deleted
        composeRule.onNodeWithContentDescription("TestItem1").assertIsNotDisplayed()
    }

    @Test
    fun finishButtonDisabledForEmptySession() {
        // Start session
        composeRule.onNodeWithContentDescription("Start practicing").performClick()
        composeRule.onNodeWithText("TestItem1").performClick()

        // Assert finish session disabled
        composeRule.onNodeWithText("Finish").assertIsNotEnabled()

        // Advance time
        fakeTimeProvider.advanceTimeBy(1.seconds)

        // Assert finish session enabled
        composeRule.onNodeWithText("Finish").assertIsEnabled()
    }

    @Test
    fun finishSession() = runTest {
        // Start session
        composeRule.onNodeWithContentDescription("Start practicing").performClick()
        composeRule.onNodeWithText("TestItem1").performClick()

        // Advance time
        fakeTimeProvider.advanceTimeBy(3.minutes)

        // Pause session
        composeRule.onNodeWithContentDescription("Pause").performClick()

        // Advance time
        fakeTimeProvider.advanceTimeBy(1.minutes)

        // Finish session
        composeRule.onNodeWithText("Finish").performClick()

        // Add rating and comment
        composeRule.onNodeWithContentDescription("Rate 5 out of 5 stars").performClick()
        composeRule.onNodeWithText("Comment (optional)").performTextInput("Perfect 5 out of 7")

        // Confirm finish
        composeRule.onNodeWithText("Save").performClick()

        // Wait for the navigation to finish
        composeRule.awaitIdle()

        // Navigate up is called
        verify(exactly = 1) {
            navController.navigateUp()
        }

        // Sessions are still empty
        val sessions = sessionsUseCases.getAll().first()
        assertThat(sessions).containsExactly(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(6),
                    breakDurationSeconds = 60,
                    rating = 5,
                    comment = "Perfect 5 out of 7",
                    createdAt = FakeTimeProvider.START_TIME + 4.minutes,
                    modifiedAt = FakeTimeProvider.START_TIME + 4.minutes,
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(7),
                            sessionId = UUIDConverter.fromInt(6),
                            libraryItemId = UUIDConverter.fromInt(2),
                            durationSeconds = 180,
                            startTimestamp = FakeTimeProvider.START_TIME,
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(2),
                            name = "TestItem1",
                            colorIndex = 1,
                            libraryFolderId = null,
                            customOrder = null,
                            createdAt = FakeTimeProvider.START_TIME,
                            modifiedAt = FakeTimeProvider.START_TIME
                        ),
                    )
                )
            )
        )
    }
}
