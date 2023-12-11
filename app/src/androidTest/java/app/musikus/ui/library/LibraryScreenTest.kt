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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.musikus.di.AppModule
import app.musikus.ui.MainActivity
import app.musikus.ui.MainViewModel
import app.musikus.ui.Screen
import com.google.android.material.composethemeadapter3.Mdc3Theme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import app.musikus.R
import app.musikus.utils.TestTags


@HiltAndroidTest
@UninstallModules(AppModule::class)
class LibraryScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val mainViewModel: MainViewModel = hiltViewModel()

            val navController = rememberNavController()
            Mdc3Theme {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Library.route
                ) {
                    composable(Screen.Library.route) {
                        LibraryScreen(mainViewModel = mainViewModel)
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
    }
}