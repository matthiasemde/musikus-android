/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 */

package app.musikus.goals.presentation

import androidx.activity.compose.setContent
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.hilt.navigation.compose.hiltViewModel
import app.musikus.core.presentation.MainActivity
import app.musikus.core.presentation.utils.TestTags
import app.musikus.library.presentation.DialogMode
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class GoalDialogTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val viewModel: GoalsViewModel = hiltViewModel()

            val dialogUiState = GoalsAddOrEditDialogUiState(
                mode = DialogMode.ADD,
                goalToEditId = null,
                initialTargetHours = 0,
                initialTargetMinutes = 0,
                libraryItems = emptyList(),
                oneShotGoal = false,
            )
            GoalDialog(
                eventHandler = { viewModel.onUiEvent(GoalsUiEvent.DialogUiEvent(it)) },
                uiState = dialogUiState,
            )
        }
    }

    @Test
    fun createNewGoalTest() {
        composeRule.onNodeWithTag(TestTags.GOAL_DIALOG_HOURS_INPUT).performTextReplacement("1")
        composeRule.onNodeWithTag(TestTags.GOAL_DIALOG_MINUTES_INPUT).performTextReplacement("30")

        // assert unit is singular
        // select week
        composeRule.onNodeWithContentDescription("Select period unit").performClick()
        composeRule.onNode(
            matcher = hasAnyAncestor(hasTestTag(TestTags.GOAL_DIALOG_PERIOD_UNIT_SELECTOR_DROPDOWN))
                    and
                    hasText("Week")
        ).performClick()


        // change period to 2
        composeRule.onNodeWithTag(TestTags.GOAL_DIALOG_PERIOD_INPUT).performTextReplacement("2")
        // select months (plural)
        composeRule.onNodeWithContentDescription("Select period unit").performClick()
        composeRule.onNode(
            matcher = hasAnyAncestor(hasTestTag(TestTags.GOAL_DIALOG_PERIOD_UNIT_SELECTOR_DROPDOWN))
                    and
                    hasText("Months")
        ).performClick()

        composeRule.onNodeWithContentDescription("Create").performClick()
    }



}