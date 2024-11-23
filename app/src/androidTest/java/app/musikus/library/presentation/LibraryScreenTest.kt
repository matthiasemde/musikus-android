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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import app.musikus.R
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.presentation.MainActivity
import app.musikus.core.presentation.MainViewModel
import app.musikus.core.presentation.utils.TestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class LibraryScreenTest {
    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

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
    fun saveNewItems_checkDefaultSortingThenNameSortingDescAndAsc() {
        val namesAndColors = listOf(
            "TestItem3" to 3,
            "TestItem1" to 9,
            "TestItem2" to 5,
        )

        namesAndColors.forEach { (name, color) ->
            composeRule.onNodeWithContentDescription("Add folder or item").performClick()
            composeRule.onNodeWithContentDescription("Add item").performClick()
            composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput(name)
            composeRule.onNodeWithContentDescription("Color $color").performClick()
            composeRule.onNodeWithContentDescription("Create").performClick()

            fakeTimeProvider.advanceTimeBy(1.seconds)
        }

        // Check if items are displayed in correct order
        var itemNodes = composeRule.onAllNodes(hasText("TestItem", substring = true))

        itemNodes.assertCountEquals(namesAndColors.size)

        for (i in namesAndColors.indices) {
            itemNodes[i].assertTextContains(namesAndColors[namesAndColors.lastIndex - i].first)
        }

        // Change sorting mode to name descending
        clickSortMode("items", "Name")

        // Check if items are displayed in correct order
        itemNodes = composeRule.onAllNodes(hasText("TestItem", substring = true))

        itemNodes.assertCountEquals(namesAndColors.size)

        for (i in namesAndColors.indices) {
            itemNodes[i].assertTextContains("TestItem${namesAndColors.size - i}")
        }

        // Change sorting mode to name ascending
        clickSortMode("items", "Name")

        // Check if items are displayed in correct order
        itemNodes = composeRule.onAllNodes(hasText("TestItem", substring = true))

        itemNodes.assertCountEquals(namesAndColors.size)

        for (i in namesAndColors.indices) {
            itemNodes[i].assertTextContains("TestItem${i + 1}")
        }
    }

    @Test
    fun saveNewFolders_checkDefaultSortingThenNameSortingDescAndAsc() {
        val names = listOf(
            "TestFolder2",
            "TestFolder3",
            "TestFolder1",
        )

        names.forEach { name ->
            composeRule.onNodeWithContentDescription("Add folder or item").performClick()
            composeRule.onNodeWithContentDescription("Add folder").performClick()
            composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput(name)
            composeRule.onNodeWithContentDescription("Create").performClick()

            fakeTimeProvider.advanceTimeBy(1.seconds)
        }

        // Check if items are displayed in correct order
        var itemNodes = composeRule.onAllNodes(hasText("TestFolder", substring = true))

        itemNodes.assertCountEquals(names.size)

        for (i in names.indices) {
            itemNodes[i].assertTextContains(names[names.lastIndex - i])
        }

        // Change sorting mode to name descending
        clickSortMode("folders", "Name")

        // Check if items are displayed in correct order
        itemNodes = composeRule.onAllNodes(hasText("TestFolder", substring = true))

        itemNodes.assertCountEquals(names.size)

        for (i in names.indices) {
            itemNodes[i].assertTextContains("TestFolder${names.size - i}")
        }

        // Change sorting mode to name ascending
        clickSortMode("folders", "Name")

        // Check if items are displayed in correct order
        itemNodes = composeRule.onAllNodes(hasText("TestFolder", substring = true))

        itemNodes.assertCountEquals(names.size)

        for (i in names.indices) {
            itemNodes[i].assertTextContains("TestFolder${i + 1}")
        }
    }
}
