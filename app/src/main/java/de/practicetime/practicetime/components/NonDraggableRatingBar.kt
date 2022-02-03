package de.practicetime.practicetime.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

class NonDraggableRatingBar(
    context: Context,
    attrs: AttributeSet
) : androidx.appcompat.widget.AppCompatRatingBar(context, attrs) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event?.action == MotionEvent.ACTION_DOWN) {
            super.onTouchEvent(
                MotionEvent.obtain(
                    event.downTime,
                    event.eventTime,
                    MotionEvent.ACTION_UP,
                    event.x,
                    event.y,
                    event.metaState
                ))
        }
        return true
    }
}