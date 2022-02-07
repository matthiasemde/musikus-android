package de.practicetime.practicetime.components

import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import android.graphics.Rect


class NumberInput(
    context: Context,
    attrs: AttributeSet,
) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    private var firstEdit = false

    init {
        val a = context.obtainStyledAttributes(attrs, de.practicetime.practicetime.R.styleable.numberInput)

        val showLeadingZeroes = a.getBoolean(de.practicetime.practicetime.R.styleable.numberInput_showLeadingZeroes, false)
        val maxValue = a.getInt(de.practicetime.practicetime.R.styleable.numberInput_maxValue, 99)

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