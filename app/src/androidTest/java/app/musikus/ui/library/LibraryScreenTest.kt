/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.library

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.musikus.R
import app.musikus.ui.MainActivity
import app.musikus.ui.MainViewModel
import app.musikus.ui.Screen
import app.musikus.utils.FakeTimeProvider
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortMode
import app.musikus.utils.TestTags
import com.google.android.material.composethemeadapter3.Mdc3Theme
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
            val viewModel: MainViewModel = hiltViewModel()

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val navController = rememberNavController()
            Mdc3Theme {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Library.route
                ) {
                    composable(Screen.Library.route) {
                        Library(
                            viewModel::onEvent,
                            uiState
                        )
                    }
                }
            }
        }
    }

    @Test
    fun clickFab_multiFabMenuIsShown() {
        composeRule.onNodeWithContentDescription("Folder").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Item").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Add").performClick()

        composeRule.onNodeWithContentDescription("Folder").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Item").assertIsDisplayed()
    }

    @Test
    fun addFolderOrItem_hintDisappears() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Check if hint is displayed initially
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertIsDisplayed()

        // Add a folder
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithContentDescription("Folder").performClick()
        composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput("Test")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if hint is not displayed anymore
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertDoesNotExist()

        // Remove the folder
        composeRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete").performClick()

        // Check if hint is displayed again
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertIsDisplayed()

        // Add an item
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithContentDescription("Item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("Test")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if hint is not displayed anymore
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertDoesNotExist()

        // Remove the item
        composeRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete").performClick()

        // Check if hint is displayed again
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertIsDisplayed()
    }

    @Test
    fun addItemToFolderFromInsideAndOutside() {

        // Add a folder
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithContentDescription("Folder").performClick()
        composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput("TestFolder")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Add an item from outside the folder
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithContentDescription("Item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("TestItem1")
        composeRule.onNodeWithContentDescription("Select folder").performClick()

        composeRule.onNode(
            matcher = hasAnyAncestor(hasTestTag(TestTags.ITEM_DIALOG_FOLDER_SELECTOR_DROPDOWN))
            and
            hasText("TestFolder")
        ).performClick()
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Open folder
        composeRule.onNodeWithText("TestFolder").performClick()

        // Check if item is displayed
        composeRule.onNodeWithText("TestItem1").assertIsDisplayed()

        // Add an item from inside the folder (folder should be pre-selected)
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("TestItem2")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if item is displayed
        composeRule.onNodeWithText("TestItem2").assertIsDisplayed()
    }

    private fun clickSortMode(sortMode: SortMode<*>) {
        val sortModeType = when(sortMode) {
            is LibraryItemSortMode -> "items"
            is LibraryFolderSortMode -> "folders"
            else -> throw Exception("Unknown sort mode type")
        }
        composeRule.onNodeWithContentDescription("Select sort mode and direction for $sortModeType").performClick()
        // Select name as sorting mode
        composeRule.onNode(
            matcher = hasAnyAncestor(hasContentDescription("List of sort modes for $sortModeType"))
                    and
                    hasText(sortMode.label)
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
            composeRule.onNodeWithContentDescription("Add").performClick()
            composeRule.onNodeWithContentDescription("Item").performClick()
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
        clickSortMode(LibraryItemSortMode.NAME)

        // Check if items are displayed in correct order
        itemNodes = composeRule.onAllNodes(hasText("TestItem", substring = true))

        itemNodes.assertCountEquals(namesAndColors.size)

        for (i in namesAndColors.indices) {
            itemNodes[i].assertTextContains("TestItem${namesAndColors.size - i}")
        }

        // Change sorting mode to name ascending
        clickSortMode(LibraryItemSortMode.NAME)

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
            composeRule.onNodeWithContentDescription("Add").performClick()
            composeRule.onNodeWithContentDescription("Folder").performClick()
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
        clickSortMode(LibraryFolderSortMode.NAME)

        // Check if items are displayed in correct order
        itemNodes = composeRule.onAllNodes(hasText("TestFolder", substring = true))

        itemNodes.assertCountEquals(names.size)

        for (i in names.indices) {
            itemNodes[i].assertTextContains("TestFolder${names.size - i}")
        }

        // Change sorting mode to name ascending
        clickSortMode(LibraryFolderSortMode.NAME)

        // Check if items are displayed in correct order
        itemNodes = composeRule.onAllNodes(hasText("TestFolder", substring = true))

        itemNodes.assertCountEquals(names.size)

        for (i in names.indices) {
            itemNodes[i].assertTextContains("TestFolder${i + 1}")
        }
    }
}