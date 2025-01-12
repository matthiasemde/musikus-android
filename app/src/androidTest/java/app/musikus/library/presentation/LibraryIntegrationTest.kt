/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.library.presentation

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.filters.SdkSuppress
import app.ScreenshotRule
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.presentation.HomeTab
import app.musikus.core.presentation.HomeTabNavType
import app.musikus.core.presentation.MainActivity
import app.musikus.core.presentation.MainViewModel
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.utils.TestTags
import app.musikus.library.presentation.libraryfolder.LibraryFolderDetailsScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import javax.inject.Inject
import kotlin.reflect.typeOf

@HiltAndroidTest
class LibraryIntegrationTest {
    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule(composeRule)

    lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val mainEventHandler = mainViewModel::onUiEvent

            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(
                navController = navController,
                startDestination = Screen.Home(tab = HomeTab.Library),
            ) {
                composable<Screen.Home>(
                    typeMap = mapOf(typeOf<HomeTab>() to HomeTabNavType),
                ) {
                    Library(
                        mainUiState = mainUiState,
                        mainEventHandler = mainEventHandler,
                        navigateToFolderDetails = { navController.navigate(it) },
                        bottomBarHeight = 0.dp
                    )
                }

                composable<Screen.LibraryFolderDetails> {
                    val folderId = UUID.fromString(it.toRoute<Screen.LibraryFolderDetails>().folderId)

                    LibraryFolderDetailsScreen(
                        mainEventHandler = mainEventHandler,
                        folderId = folderId,
                        navigateUp = navController::navigateUp
                    )
                }
            }
        }
    }

    @Test
    @SdkSuppress(excludedSdks = [29])
    fun addItemToFolderFromInsideAndOutside() {
        // Add a folder
        composeRule.onNodeWithContentDescription("Add folder or item").performClick()
        composeRule.onNodeWithContentDescription("Add folder").performClick()
        composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput("TestFolder")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Add an item from outside the folder
        composeRule.onNodeWithContentDescription("Add folder or item").performClick()
        composeRule.onNodeWithContentDescription("Add item").performClick()
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
}
