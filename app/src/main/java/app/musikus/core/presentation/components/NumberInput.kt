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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.BaselineShift
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
    val minValue: Int,
    val maxValue: Int,
) {
    init {
        require(maxValue > minValue) { "maxValue must be larger than minValue" }
        if (initialValue != null) {
            require(initialValue in minValue..maxValue) { "initialValue not in allowed range" }
        }
    }
    var currentValue = mutableStateOf(initialValue)

    companion object {
        /**
         *  Tnables saving the state on configuration changes via rememberSaveable
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
                    initialValue = it[0] as Int?,
                    minValue = it[1] as Int,
                    maxValue = it[2] as Int
                )
            }
        )
    }
}

@Composable
fun rememberNumberInputState(
    initialValue: Int? = null,
    minValue: Int = 0,
    maxValue: Int = 99,
): NumberInputState = rememberSaveable(
    saver = NumberInputState.Saver()
) {
    NumberInputState(
        initialValue = initialValue,
        minValue = minValue,
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
    val displayValue = if (focused || !padStart) {
        state.currentValue.value?.toString() ?: ""
    } else {
        (state.currentValue.value?.toString() ?: "").padStart(maxLength, '0')
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
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            onValueChange = { newValue ->
                val number = newValue.toIntOrNull()
                if (number in state.minValue..state.maxValue || number == null) {
                    state.currentValue.value = number
                    onValueChanged(number)
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
                        Row(horizontalArrangement = Arrangement.Center){
                            innerTextField()
                        }
                        // placeholder preserves width
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


@MusikusPreviewElement1
@Composable
private fun NumberInputPreview(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    MusikusThemedPreview(theme) {
        NumberInput(
            state = rememberNumberInputState(initialValue = 0, minValue = 0, maxValue = 99),
            imeAction = ImeAction.Done,
            label = { modifier ->
                Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
            }
        )
    }
}