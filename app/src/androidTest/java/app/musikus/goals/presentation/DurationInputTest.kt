/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 */

package app.musikus.goals.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import app.musikus.core.presentation.components.DurationInput
import app.musikus.core.presentation.components.NumberInputState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class DurationInputTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    /**
     * This test checks if the user can seamlessly enter a duration in hours and minutes. "Seamless" means that the user can move across
     * the input fields as he is typing without having to manually switch focus.
     *
     * This only works when initialValue is zero (like in GoalDialog)
     * since only like this all input fields are empty when focused due to the trimming of leading zeros.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun checkSeamlessEntering() {

        val hoursState = NumberInputState(
            initialValue = 0,
            minValue = 0,
            maxValue = 99
        )

        val minutesState = NumberInputState(
            initialValue = 0,
            minValue = 0,
            maxValue = 59
        )

        composeRule.setContent {
            DurationInput(
                hoursState = hoursState,
                minutesState = minutesState,
                requestFocusOnInit = true
            )
        }

        composeRule.onNodeWithContentDescription("Target hours input").apply {
            assertIsFocused()
            performTextInput("12")
            // this asserts that automatically the next field is focused
            assertIsNotFocused()
        }

        composeRule.onNodeWithContentDescription("Target minutes input").apply {
            assertIsFocused()
            performTextInput("34")
            performImeAction()
            assertIsNotFocused()
        }

        // assert the correct state values
        assert(hoursState.currentValue.value == 12)
        assert(minutesState.currentValue.value == 34)
    }
}