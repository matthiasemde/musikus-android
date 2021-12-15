package de.practicetime.practicetime.components

import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import android.R

import android.content.res.TypedArray




class NumberInput(
    context: Context,
    attrs: AttributeSet,
) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    init {
        val a = context.obtainStyledAttributes(attrs, de.practicetime.practicetime.R.styleable.numberInput)

        val showLeadingZeroes = a.getBoolean(de.practicetime.practicetime.R.styleable.numberInput_showLeadingZeroes, false)
        val maxValue = a.getInt(de.practicetime.practicetime.R.styleable.numberInput_maxValue, 99)

        filters = arrayOf(InputFilterMax(maxValue))

        val maxLength = maxValue.toString().length

        addTextChangedListener {
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

    private fun showKeyboard() {
        (context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private class InputFilterMax(
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