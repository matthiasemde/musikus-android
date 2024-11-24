/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.hilt.navigation.compose.hiltViewModel
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.presentation.MainActivity
import app.musikus.core.presentation.MainViewModel
import app.musikus.core.presentation.utils.TestTags
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.library.presentation.libraryfolder.LibraryFolderDetailsScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class LibraryFolderDetailsScreenTest {
    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @Inject lateinit var libraryUseCases: LibraryUseCases

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val mainEventHandler = mainViewModel::onUiEvent

            runBlocking {
                libraryUseCases.addFolder(
                    LibraryFolderCreationAttributes("TestFolder1")
                )
            }

            LibraryFolderDetailsScreen(
                mainEventHandler = mainEventHandler,
                folderId = UUIDConverter.fromInt(1),
                navigateUp = {}
            )
        }
    }

    @Test
    fun addItemToFolder() {
        // Add an item from inside the folder (folder should be pre-selected)
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("TestItem2")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if item is displayed
        composeRule.onNodeWithText("TestItem2").assertIsDisplayed()
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
    fun saveNewItems_checkDefaultSortingThenNameSortingDescAndAsc() = runTest {
        val namesAndColors = listOf(
            "TestItem3" to 3,
            "TestItem1" to 9,
            "TestItem2" to 5,
        )

        namesAndColors.forEach { (name, color) ->
            composeRule.onNodeWithContentDescription("Add item").performClick()
            composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput(name)
            composeRule.onNodeWithContentDescription("Color $color").performClick()
            composeRule.onNodeWithContentDescription("Create").performClick()

            fakeTimeProvider.advanceTimeBy(1.seconds)
        }

        // Check if items are displayed in correct order
        var itemNodes = composeRule.onAllNodes(hasText("TestItem", substring = true))

        itemNodes.assertCountEquals(3)

        for (i in namesAndColors.indices) {
            itemNodes[i].assertTextContains(namesAndColors[namesAndColors.lastIndex - i].first)
        }

        // Change sorting mode to name descending
        clickSortMode("items", "Name")

        composeRule.awaitIdle()

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
    fun editFolder() {
        // Edit folder name
        composeRule.onNodeWithContentDescription("Edit").performClick()
        composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).apply {
            performTextClearance()
            performTextInput("TestFolder2")
        }
        composeRule.onNodeWithText("Edit").performClick()

        // Check if folder name is displayed
        composeRule.onNodeWithText("TestFolder2").assertIsDisplayed()
    }

    @Test
    fun editItem() {
        // Add an item from inside the folder (folder should be pre-selected)
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("TestItem1")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Edit item by clicking it
        composeRule.onNodeWithText("TestItem1").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).apply {
            performTextClearance()
            performTextInput("TestItem2")
        }
        composeRule.onNodeWithText("Edit").performClick()

        // Check if edited item name is displayed
        composeRule.onNodeWithText("TestItem2").assertIsDisplayed()

        // Edit item using action mode
        composeRule.onNodeWithText("TestItem2").performTouchInput { longClick() }
        composeRule.onNode(
            matcher = hasContentDescription("Edit")
                and hasAnySibling(hasText("1 items selected"))
        ).performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).apply {
            performTextClearance()
            performTextInput("TestItem3")
        }
        composeRule.onNodeWithText("Edit").performClick()

        // Check if edited item name is displayed
        composeRule.onNodeWithText("TestItem3").assertIsDisplayed()
    }
}
