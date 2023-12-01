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

package app.musikus.components

import android.content.Context
import android.graphics.Rect
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.widget.addTextChangedListener
import app.musikus.R


class NumberInput(
    context: Context,
    attrs: AttributeSet,
) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    private var firstEdit = false

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.numberInput)

        val showLeadingZeroes = a.getBoolean(R.styleable.numberInput_showLeadingZeroes, false)
        val maxValue = a.getInt(R.styleable.numberInput_maxValue, 99)

        filters = arrayOf(InputFilterMax(maxValue))

        val maxLength = maxValue.toString().length

        addTextChangedListener {
            if(firstEdit) {
                it?.replace(0, it.length - 1, "", 0, 0)
                firstEdit = false
            }
            val diff = maxLength - (it?.length ?: 0)
            if (diff < 0)
                this.setText(it?.drop(-diff))
            else if (showLeadingZeroes && diff > 0)
                this.setText(it?.padStart(maxLength, '0'))
            this.setSelection(text?.length ?: 0)
        }

        setOnTouchListener { _, _ ->
            performClick()
            setSelection(text?.length ?: 0)
            requestFocus()
            showKeyboard()
            true
        }
    }

    fun value() = text.toString().trim().toIntOrNull()

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if(focused) {
            firstEdit = true
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    private fun showKeyboard() {
        (context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private inner class InputFilterMax(
        private var max: Int
    ) : InputFilter {
        init {
            assert(max >= 1) {
                Log.e("Assertion failed", "Maximum has to be larger than 0")
            }
        }

        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence {
            try {
                if(firstEdit) return if(source.toString().toInt() in 0..max) source else "0"
                val input = (dest.slice(0 until dstart).toString()
                        + source.toString()
                        + dest.slice(dend until dest.length).toString()
                        )
                if (input.toInt() in 0..max) return source
            } catch (nfe: NumberFormatException) { }
            return ""
        }
    }
}

@Composable
fun NumberInput(
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
        color = if(isFocused)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface,
        baselineShift = BaselineShift(0f),
        textDecoration = if(underlined) TextDecoration.Underline else TextDecoration.None,
    )
    CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(
        Color.Transparent,
        Color.Transparent
    )) {
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
                                if (showLeadingZero)
                                    newString.padStart(maxLength, '0')
                                else
                                    newString
                            )
                            if(newString.length == maxLength) {
                                when(imeAction) {
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
                        if(value.isEmpty() && placeHolder != null) {
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
            Surface(modifier = Modifier
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
