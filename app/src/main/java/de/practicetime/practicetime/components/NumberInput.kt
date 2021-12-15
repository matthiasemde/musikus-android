package de.practicetime.practicetime.components

import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener

class NumberInput(
    context: Context,
    attrs: AttributeSet,
) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    init {
        filters = arrayOf(InputFilterMax(99))
        addTextChangedListener {
            val diff = 2 - (it?.length ?: 0)
            if(diff < 0)
                this.setText(it?.drop(-diff))
            else if(diff > 0)
                this.setText(it?.padStart(2, '0'))
            this.setSelection(2)
        }
        setOnTouchListener { _, _ ->
            performClick()
            setSelection(2)
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