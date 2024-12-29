/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing

@Composable
fun NumberInputOld(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    textSize: TextUnit,
    showLeadingZero: Boolean = false,
    minValue: Int = 0,
    maxValue: Int = 99,
    imeAction: ImeAction = ImeAction.Done,
    label: @Composable ((Modifier) -> Unit)? = null,
    placeHolder: String? = null,
    underlined: Boolean = false,
) {
    val maxLength = maxValue.toString().length
    val localFocusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val textStyle = TextStyle(
        fontSize = textSize,
        fontWeight = FontWeight.Bold,
        color = if (isFocused) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        baselineShift = BaselineShift(0f),
        textDecoration = if (underlined) TextDecoration.Underline else TextDecoration.None,
    )
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            Color.Transparent,
            Color.Transparent
        )
    ) {
        Box {
            Row {
                BasicTextField(
                    modifier = modifier
                        .width(IntrinsicSize.Min)
                        .height(IntrinsicSize.Min)
                        .alignByBaseline()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = textStyle,
                    value = TextFieldValue(
                        text = value,
                        selection = TextRange(maxLength)
                    ),
                    onValueChange = { newValue ->
                        val newInt = newValue.text.toIntOrNull() ?: 0
                        if (newInt in minValue..maxValue) {
                            val newString = newInt.toString()
                            onValueChange(
                                if (showLeadingZero) {
                                    newString.padStart(maxLength, '0')
                                } else {
                                    newString
                                }
                            )
                            if (newString.length == maxLength) {
                                when (imeAction) {
                                    ImeAction.Next -> localFocusManager.moveFocus(FocusDirection.Next)
                                    ImeAction.Done -> localFocusManager.clearFocus()
                                    else -> localFocusManager.clearFocus()
                                }
                            }
                        }
                    },
                    cursorBrush = SolidColor(Color.Unspecified),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { localFocusManager.moveFocus(FocusDirection.Next) },
                        onDone = { localFocusManager.clearFocus() }
                    ),
                    decorationBox = { innerTextField ->
                        innerTextField()
                        if (value.isEmpty() && placeHolder != null) {
                            Text(
                                text = placeHolder,
                                style = textStyle,
                            )
                        }
                    },
                )

                if (label != null) {
                    label(Modifier.alignByBaseline())
                }
            }
            Surface(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { focusRequester.requestFocus() },
                color = Color.Transparent,
            ) {}
        }
    }
}



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
    padStart: Boolean = true
) {
    val localFocusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val containerColor = if (!focused) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    // maximum length in characters
    val maxLength = state.maxValue.toString().length

    // actual string to display
    val displayValue = if (padStart) {
        (state.currentValue.intValue.toString()).padStart(maxLength, '0')
    } else {
        state.currentValue.intValue.toString()
    }

    Row {
        BasicTextField(
            modifier = modifier
                .alignByBaseline()
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .padding(MaterialTheme.spacing.extraSmall)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            value = displayValue,
            textStyle = if (focused) {
                textStyle.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                textStyle.copy(color = MaterialTheme.colorScheme.onSurface)
            },
            onValueChange = { newValue ->
                val number = newValue.toIntOrNull()
                if (number != null && number in state.minValue..state.maxValue) {
                    state.currentValue.intValue = number
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
                        Row(horizontalArrangement = Arrangement.Center){
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
class NumberInputState (
    initialValue: Int,
    val minValue: Int,
    val maxValue: Int,
) {
    init {
        require(maxValue > minValue) { "maxValue must be larger than minValue" }
        require(initialValue in minValue..maxValue) { "initialValue not in allowed range" }
    }

    // the value (as int) of the input field which is displayed (as string, may be padded)
    val currentValue = mutableIntStateOf(initialValue)

    companion object {
        /**
         *  Tunables saving the state on configuration changes via rememberSaveable
         */
        fun Saver(): Saver<NumberInputState, *> = Saver(
            save = {
                listOf(
                    it.currentValue.intValue,
                    it.minValue,
                    it.maxValue
                )
            },
            restore = {
                NumberInputState(
                    initialValue = it[0],
                    minValue = it[1],
                    maxValue = it[2]
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
    initialValue: Int,
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
    MusikusThemedPreview() {
        NumberInput(
            state = rememberNumberInputState(
                initialValue = 42,
                minValue = 0,
                maxValue = 99
            ),
            imeAction = ImeAction.Done,
            label = { modifier ->
                Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
            }
        )
    }
}