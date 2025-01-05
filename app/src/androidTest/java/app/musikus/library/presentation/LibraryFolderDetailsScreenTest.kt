/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.test.filters.SdkSuppress
import app.ScreenshotRule
import app.assertNodesInVerticalOrder
import app.assertWithLease
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
import kotlinx.coroutines.flow.first
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

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule(composeRule)

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
        composeRule.onNodeWithText("TestItem2").assertWithLease { assertIsDisplayed() }
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

        namesAndColors.forEachIndexed { index, (name, color) ->
            composeRule.onNodeWithContentDescription("Add item").performClick()
            composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput(name)
            composeRule.onNodeWithContentDescription("Color $color").performClick()
            composeRule.onNodeWithContentDescription("Create").performClick()

            // Suspend execution until the item is added to avoid race conditions
            libraryUseCases.getAllItems().first { it.size == index + 1 }

            fakeTimeProvider.advanceTimeBy(1.seconds)
        }

        // Check if items are displayed in correct order
        composeRule.assertWithLease {
            assertNodesInVerticalOrder(
                composeRule.onNodeWithText("TestItem2"),
                composeRule.onNodeWithText("TestItem1"),
                composeRule.onNodeWithText("TestItem3")
            )
        }

        // Change sorting mode to name descending
        clickSortMode("items", "Name")

        // Check if items are displayed in correct order
        composeRule.assertWithLease {
            assertNodesInVerticalOrder(
                composeRule.onNodeWithText("TestItem3"),
                composeRule.onNodeWithText("TestItem2"),
                composeRule.onNodeWithText("TestItem1")
            )
        }

        // Change sorting mode to name ascending
        clickSortMode("items", "Name")

        // Check if items are displayed in correct order
        composeRule.assertWithLease {
            assertNodesInVerticalOrder(
                composeRule.onNodeWithText("TestItem1"),
                composeRule.onNodeWithText("TestItem2"),
                composeRule.onNodeWithText("TestItem3")
            )
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
        composeRule.onNodeWithText("TestFolder2").assertWithLease { assertIsDisplayed() }
    }

    @Test
    @SdkSuppress(excludedSdks = [29])
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
        composeRule.onNodeWithText("TestItem2").assertWithLease { assertIsDisplayed() }

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
        composeRule.onNodeWithText("TestItem3").assertWithLease { assertIsDisplayed() }
    }

    @Test
    @SdkSuppress(excludedSdks = [29])
    fun deleteItem() = runTest {
        // Add an item
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("TestItem1")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Delete item by clicking it
        composeRule.onNodeWithText("TestItem1").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.onNodeWithContentDescription("Delete forever (1)").performClick()

        // Check if item is no longer displayed
        composeRule.onNodeWithText("TestItem1").assertDoesNotExist()
    }
}
