package app.musikus.goals.presentation

import androidx.compose.material3.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.ImeAction
import app.musikus.core.presentation.components.NumberInput
import app.musikus.core.presentation.components.NumberInputState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NumberInputTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun checkMinValueAsDefault() {

        val minValue = 2

        val numberInputState = NumberInputState (
            initialValue = 50,
            minValue = minValue,
            maxValue = 100,
        )

        val focusRequester = FocusRequester()

        composeRule.setContent {
            NumberInput(
                state = numberInputState,
                inputTestTag = "NumberInputTest",
                padStart = false,
                imeAction = ImeAction.Next,
            )

            // dummy text field to steal focus from NumberInput
            TextField("dummy", onValueChange = {}, modifier = Modifier.focusRequester(focusRequester))
        }

        // assert that initialValue has been entered
        composeRule.onNodeWithText("50").apply {
            assertExists()
            performTextReplacement("")
        }

        focusRequester.requestFocus()

        // assert that minValue has been entered
        composeRule.onNodeWithText(minValue.toString()).assertExists()
        // assert that currentValue is minValue
        assert(numberInputState.currentValue.value == minValue)
    }

}