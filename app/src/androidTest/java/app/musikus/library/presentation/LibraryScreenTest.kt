/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.library.presentation

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import app.ScreenshotRule
import app.assertNodesInHorizontalOrder
import app.assertNodesInVerticalOrder
import app.musikus.R
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.presentation.MainActivity
import app.musikus.core.presentation.MainViewModel
import app.musikus.core.presentation.utils.TestTags
import app.musikus.library.domain.usecase.LibraryUseCases
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class LibraryScreenTest {
    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @Inject lateinit var libraryUseCases: LibraryUseCases

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val mainEventHandler = mainViewModel::onUiEvent

            Library(
                mainUiState = mainUiState,
                mainEventHandler = mainEventHandler,
                navigateToFolderDetails = { },
                bottomBarHeight = 0.dp
            )
        }
    }

    @Test
    fun clickFab_multiFabMenuIsShown() {
        composeRule.onNodeWithContentDescription("Add folder").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Add item").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Add folder or item").performClick()

        composeRule.onNodeWithContentDescription("Add folder").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add item").assertIsDisplayed()
    }

    @Test
    @SdkSuppress(excludedSdks = [29])
    fun addFolderOrItem_hintDisappears() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Check if hint is displayed initially
        composeRule.onNodeWithText(context.getString(R.string.library_screen_hint)).assertIsDisplayed()

        // Add a folder
        composeRule.onNodeWithContentDescription("Add folder or item").performClick()
        composeRule.onNodeWithContentDescription("Add folder").performClick()
        composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput("Test")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if hint is not displayed anymore
        composeRule.onNodeWithText(context.getString(R.string.library_screen_hint)).assertDoesNotExist()

        // Remove the folder
        composeRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.onNodeWithContentDescription("Delete forever (1)").performClick()

        // Check if hint is displayed again
        composeRule.onNodeWithText(context.getString(R.string.library_screen_hint)).assertIsDisplayed()

        // Add an item
        composeRule.onNodeWithContentDescription("Add folder or item").performClick()
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("Test")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if hint is not displayed anymore
        composeRule.onNodeWithText(context.getString(R.string.library_screen_hint)).assertDoesNotExist()

        // Remove the item
        composeRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.onNodeWithContentDescription("Delete forever (1)").performClick()

        // Check if hint is displayed again
        composeRule.onNodeWithText(context.getString(R.string.library_screen_hint)).assertIsDisplayed()
    }

    private fun clickSortMode(
        sortModeType: String,
        sortMode: String
    ) {
        composeRule.onNodeWithContentDescription("Select sort mode and direction for $sortModeType").performClick()
        // Select name as sorting mode
        composeRule.onNode(
            matcher = hasAnyAncestor(hasContentDescription("List of sort modes for $sortModeType"))
                and
                hasText(sortMode)
        ).performClick()
    }

    @Test
    @SdkSuppress(excludedSdks = [29])
    fun saveNewItems_checkDefaultSortingThenNameSortingDescAndAsc() = runTest {
        val namesAndColors = listOf(
            "TestItem3" to 3,
            "TestItem1" to 9,
            "TestItem2" to 5,
        )

        namesAndColors.forEachIndexed { index, (name, color) ->
            composeRule.onNodeWithContentDescription("Add folder or item").performClick()
            composeRule.onNodeWithContentDescription("Add item").performClick()
            composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput(name)
            composeRule.onNodeWithContentDescription("Color $color").performClick()
            composeRule.onNodeWithContentDescription("Create").performClick()

            // Suspend execution until the item is added to avoid race conditions
            libraryUseCases.getAllItems().first { it.size == index + 1 }

            fakeTimeProvider.advanceTimeBy(1.seconds)
        }

        // Check if items are displayed in correct order
        assertNodesInVerticalOrder(
            composeRule.onNodeWithText("TestItem2"),
            composeRule.onNodeWithText("TestItem1"),
            composeRule.onNodeWithText("TestItem3"),
        )

        // Change sorting mode to name descending
        clickSortMode("items", "Name")

        // Check if items are displayed in correct order
        assertNodesInVerticalOrder(
            composeRule.onNodeWithText("TestItem3"),
            composeRule.onNodeWithText("TestItem2"),
            composeRule.onNodeWithText("TestItem1"),
        )

        // Change sorting mode to name ascending
        clickSortMode("items", "Name")

        // Check if items are displayed in correct order
        assertNodesInVerticalOrder(
            composeRule.onNodeWithText("TestItem1"),
            composeRule.onNodeWithText("TestItem2"),
            composeRule.onNodeWithText("TestItem3"),
        )
    }

    @Test
    @SdkSuppress(excludedSdks = [29])
    fun saveNewFolders_checkDefaultSortingThenNameSortingDescAndAsc() = runTest {
        val names = listOf(
            "TestFolder2",
            "TestFolder3",
            "TestFolder1",
        )

        names.forEachIndexed { index, name ->
            composeRule.onNodeWithContentDescription("Add folder or item").performClick()
            composeRule.onNodeWithContentDescription("Add folder").performClick()
            composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput(name)
            composeRule.onNodeWithContentDescription("Create").performClick()

            // Suspend execution until the folder is added to avoid race conditions
            libraryUseCases.getSortedFolders().first { it.size == index + 1 }

            fakeTimeProvider.advanceTimeBy(1.seconds)
        }

        // Check if folders are displayed in correct order
        assertNodesInHorizontalOrder(
            composeRule.onNodeWithText("TestFolder1"),
            composeRule.onNodeWithText("TestFolder3"),
            composeRule.onNodeWithText("TestFolder2"),
        )

        // Change sorting mode to name descending
        clickSortMode("folders", "Name")

        // Check if folders are displayed in correct order
        assertNodesInHorizontalOrder(
            composeRule.onNodeWithText("TestFolder3"),
            composeRule.onNodeWithText("TestFolder2"),
            composeRule.onNodeWithText("TestFolder1"),
        )

        // Change sorting mode to name ascending
        clickSortMode("folders", "Name")

        // Check if folders are displayed in correct order
        assertNodesInHorizontalOrder(
            composeRule.onNodeWithText("TestFolder1"),
            composeRule.onNodeWithText("TestFolder2"),
            composeRule.onNodeWithText("TestFolder3"),
        )
    }
}
