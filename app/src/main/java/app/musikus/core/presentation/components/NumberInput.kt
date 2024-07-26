/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.spacing
import app.musikus.settings.domain.ColorSchemeSelections


@Composable
fun NumberInputOld(
    modifier: Modifier = Modifier,
    state: MutableState<Int>,
    textSize: TextUnit,
    minValue: Int = 0,
    maxValue: Int = 99,
    imeAction: ImeAction = ImeAction.Done,
    label: @Composable ((Modifier) -> Unit)? = null,
) {
    val localFocusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val textStyle = TextStyle(
        fontSize = textSize,
        fontWeight = FontWeight.Bold,
        color = if (isFocused)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface,
        baselineShift = BaselineShift(0f),
    )
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            Color.Transparent,
            Color.Transparent
        )
    ) {
//        Box {
//            Row {
//                BasicTextField(
//                    modifier = modifier
//                    value = TextFieldValue(
//                        text = state.value.toString()
//                    ),
//                        .alignByBaseline()
//                        .focusRequester(focusRequester),
//                        .onFocusChanged { isFocused = it.isFocused },
//                    textStyle = textStyle,
//                    onValueChange = { newValue ->
//                        val number = newValue.text.toIntOrNull() ?: 0
//                        if (number in minValue..maxValue) {
//                            state.value = number
//                        }
//
//                        val newInt = newValue.text.toIntOrNull() ?: 0
//                        if (newInt in minValue..maxValue) {
//                            val newString = newInt.toString()
//                            onValueChange(
//                                if (showLeadingZero)
//                                    newString.padStart(maxLength, '0')
//                                else
//                                    newString
//                            )
//                            if(newString.length == maxLength) {
//                                when(imeAction) {
//                                    ImeAction.Next -> localFocusManager.moveFocus(FocusDirection.Next)
//                                    ImeAction.Done -> localFocusManager.clearFocus()
//                                    else -> localFocusManager.clearFocus()
//                                }
//                            }
//                        }
//                    },
//                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
//                    keyboardOptions = KeyboardOptions(
//                        keyboardType = KeyboardType.Number,
//                        imeAction = imeAction
//                    ),
//                    keyboardActions = KeyboardActions(
//                        onNext = { localFocusManager.moveFocus(FocusDirection.Next) },
//                        onDone = { localFocusManager.clearFocus() }
//                    ),
//                )
//
//                if (label != null) {
//                    label(Modifier.alignByBaseline())
//                }
//            }
//            Surface(modifier = Modifier
//                .matchParentSize()
//                .clickable(
//                    interactionSource = remember { MutableInteractionSource() },
//                    indication = null,
//                ) { focusRequester.requestFocus() },
//                color = Color.Transparent,
//            ) {}
//        }
    }
}


@Stable
class NumberInputState (
    initialValue: Int? = null,
    val maxValue: Int,
) {
    init {
        require(maxValue > 0) { "maxValue must be larger than zero" }
        if (initialValue != null) {
            require(initialValue in 0..maxValue) { "initialValue not in allowed range" }
        }
    }
    val currentValue = mutableStateOf(TextFieldValue(initialValue?.toString() ?: ""))

    val numericValue: Int
        get() = currentValue.value.text.toIntOrNull() ?: 0

    companion object {
        /**
         *  Tnables saving the state on configuration changes via rememberSaveable
         */
        fun Saver(): Saver<NumberInputState, *> = Saver(
            save = {
                listOf(
                    it.currentValue.value,
                    it.maxValue
                )
            },
            restore = {
                NumberInputState(
                    initialValue = (it[0] as TextFieldValue).text.toIntOrNull(),
                    maxValue = it[2] as Int
                )
            }
        )
    }
}

@Composable
fun rememberNumberInputState(
    initialValue: Int? = null,
    maxValue: Int = 99,
): NumberInputState = rememberSaveable(
    saver = NumberInputState.Saver()
) {
    NumberInputState(
        initialValue = initialValue,
        maxValue = maxValue,
    )
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
    padStart: Boolean = true,
    onValueChanged: (Int?) -> Unit = {},
) {
    val localFocusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val containerColor = if (!focused) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val maxLength = state.maxValue.toString().length

    Row {
        BasicTextField(
            modifier = modifier
                .alignByBaseline()
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .padding(MaterialTheme.spacing.extraSmall)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    focused = it.isFocused
                },
            value = state.currentValue.value,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = if(focused) TextAlign.Center else TextAlign.Right),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
            onValueChange = { newValue ->
                /**
                 * Remember that entering a TextField triggers onValueChange if the cursor
                 * position changed from the last time the field was focused!
                 * */

                // If the box is full and the user enters a number at the beginning,
                // we overwrite all the text with the new number
                var newValueOnlyNumbers = newValue.text.filter { it.isDigit() }
                if (newValueOnlyNumbers.length > maxLength) {
                    // collapsed means cursor is just a vertical line (=no selection)
                    if(newValue.selection.collapsed && newValue.selection.start == 1) {
                        newValueOnlyNumbers = newValueOnlyNumbers.take(1)
                    } else {
                        return@BasicTextField
                    }
                }

                // check if the number is in the allowed range
                val number = newValueOnlyNumbers.toIntOrNull() ?: 0
                if (number <= state.maxValue) {
                    state.currentValue.value = newValue.copy(newValueOnlyNumbers)
                    onValueChanged(number)
                    println("onValueChanged: $number")
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
                    border = if (focused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(Modifier.padding(MaterialTheme.spacing.small)) {
                        innerTextField()
                        if (!focused && (padStart || state.currentValue.value.text.isEmpty())) {
                            // pad with leading zeros if needed
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Left,
                                text = "0".repeat(state.maxValue.toString().length - state.currentValue.value.text.length),
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // invisible TextField preserves width when the input is empty
                        if (padStart) {
                            Text(
                                text = state.maxValue.toString(),
                                style = textStyle,
                                color = Color.Transparent
                            )
                        }
                    }
                }
            }
        )
        if (label != null) {
            label(Modifier.alignByBaseline())
        }
    }
}


@MusikusPreviewElement1
@Composable
private fun NumberInputPreview(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        NumberInput(
            state = rememberNumberInputState(initialValue = 0, maxValue = 99),
            imeAction = ImeAction.Done,
            label = { modifier ->
                Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
            }
        )
    }
}