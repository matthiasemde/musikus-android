/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing

/**
 * NumberInput TextField, inspired by Material TimePicker
 * https://m3.material.io/components/time-pickers/specs
 */
@Composable
fun NumberInput(
    modifier: Modifier = Modifier,
    state: NumberInputState,
    textStyle: TextStyle = MaterialTheme.typography.displaySmall,
    imeAction: ImeAction = ImeAction.Done,
    label: @Composable ((Modifier) -> Unit)? = null,
    padStart: Boolean = true,
    onEntryComplete: (() -> Unit)? = null,
    onBackspaceWhenEmpty: (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    contentDescr: String = "",
) {
    val localFocusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }

    val containerColor = if (!focused) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    // maximum length in characters
    val maxLength = state.maxValue.toString().length

    // backup of last valid value, used to detect if user actually changed input
    var lastValue = state.currentValue.value

    // actual string to display. null = empty string
    val valueString = state.currentValue.value?.toString() ?: ""
    val displayValue: MutableState<TextFieldValue> = remember {
        if (padStart && !focused) {
            mutableStateOf(TextFieldValue(valueString.padStart(maxLength, '0')))
        } else {
            mutableStateOf(TextFieldValue(valueString))
        }
    }

    Row {
        BasicTextField(
            modifier = modifier
                .semantics {
                    contentDescription = contentDescr
                }
                .alignByBaseline()
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .padding(MaterialTheme.spacing.extraSmall)
                .focusRequester(focusRequester)
                .onKeyEvent { event ->
                    // trigger onBackspaceWhenEmpty, e.g. to move focus to previous field if user deletes all input
                    if (event.key == Key.Backspace && state.currentValue.value == null && lastValue == null) {
                        onBackspaceWhenEmpty?.invoke()
                    }
                    false
                }
                .onFocusChanged {
                    focused = it.isFocused

                    // add / remove padding on focus change
                    val newText = if (!focused && padStart) {
                        displayValue.value.text.padStart(maxLength, '0')
                    } else {
                        displayValue.value.text.trimStart('0')
                    }
                    displayValue.value = displayValue.value.copy(text = newText)

                    // if focus is lost and no value was entered, set to minValue
                    if (!focused && state.currentValue.value == null) {
                        state.currentValue.value = state.minValue
                        displayValue.value = displayValue.value.copy(text = state.minValue.toString())
                    }
                },
            value = displayValue.value,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = if (focused) {
                textStyle.copy(color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
            } else {
                textStyle.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            },
            onValueChange = { newValue ->
                /*
                 * Remember that entering a TextField triggers onValueChange if the cursor
                 * position changed from the last time the field was focused!
                 * */

                var newText = newValue.text

                // If the box is full (= entered over max length) and the user enters a number
                // at the beginning (= selection collapsed and start at 1) [collapsed means cursor is just a vertical line (=no selection)],
                // we overwrite all the text with the new number
                if (newText.length > maxLength && newValue.selection.collapsed && newValue.selection.start == 1) {
                    newText = newText.take(1)
                }

                // return/ignore if invalid input
                val number = newText.toIntOrNull()
                if (number !in state.minValue..state.maxValue && number != null) {
                    return@BasicTextField
                }

                lastValue = state.currentValue.value
                // assign valid value to state
                state.currentValue.value = number

                // update display state
                val newTextFieldContent = number?.toString() ?: ""
                // copy all properties of newValue to retain cursor position, but change text
                displayValue.value = newValue.copy(text = newTextFieldContent)

                // trigger onEntryComplete, e.g. to move focus to next field if max length is reached.
                // only if value actually changed, a.k.a. if user actually changed input (see first comment)
                if (lastValue != state.currentValue.value && newValue.selection.end == maxLength) {
                    onEntryComplete?.invoke()
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { localFocusManager.moveFocus(FocusDirection.Next) },
                onDone = { localFocusManager.clearFocus() }
            ),
            decorationBox = { innerTextField ->
                Surface(
                    color = containerColor,
                    shape = MaterialTheme.shapes.small,
                    border = if (focused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Box(Modifier.padding(MaterialTheme.spacing.small)) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            innerTextField()
                        }
                        // invisible placeholder preserves width
                        Text(
                            text = if (padStart) "9".padStart(maxLength, '9') else "9",
                            style = textStyle,
                            color = Color.Transparent
                        )
                    }
                }
            }
        )
        if (label != null) {
            label(Modifier.alignByBaseline())
        }
    }
}

@Stable
class NumberInputState(
    initialValue: Int?,
    val minValue: Int,
    val maxValue: Int,
) {
    init {
        require(maxValue > minValue) { "maxValue must be larger than minValue" }
        if (initialValue != null) {
            require(initialValue in minValue..maxValue) { "initialValue not in allowed range" }
        }
    }

    // the value (as int) of the input field which is displayed (as string, may be padded). Null means no text entered.
    val currentValue: MutableState<Int?> = mutableStateOf(initialValue)

    companion object {
        /**
         *  Tunables saving the state on configuration changes via rememberSaveable
         */
        fun Saver(): Saver<NumberInputState, *> = Saver(
            save = {
                listOf(
                    it.currentValue.value,
                    it.minValue,
                    it.maxValue
                )
            },
            restore = {
                NumberInputState(
                    initialValue = it[0],
                    minValue = it[1] as Int,
                    maxValue = it[2] as Int
                )
            }
        )
    }
}

/**
 * RememberSaveable wrapper for NumberInputState.
 * Use to generate a NumberInputState instance which survives configuration changes.
 */
@Composable
fun rememberNumberInputState(
    initialValue: Int?,
    minValue: Int = 0,
    maxValue: Int,
): NumberInputState = rememberSaveable(
    saver = NumberInputState.Saver()
) {
    NumberInputState(
        initialValue = initialValue,
        minValue = minValue,
        maxValue = maxValue,
    )
}

@MusikusPreviewElement1
@Composable
private fun NumberInputPreview() {
    MusikusThemedPreview {
        NumberInput(
            state = rememberNumberInputState(
                initialValue = 42,
                minValue = 0,
                maxValue = 99
            ),
            padStart = false,
            imeAction = ImeAction.Done,
            label = { modifier ->
                Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
            },
        )
    }
}
