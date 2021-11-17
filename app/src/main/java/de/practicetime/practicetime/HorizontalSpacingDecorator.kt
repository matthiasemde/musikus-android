package de.practicetime.practicetime

import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalSpacingDecoration(
    private val numberOfRows: Int,
    private val horizontalSpacing: Int,
) : RecyclerView.ItemDecoration() {

    // value resource later on
    private val outerSpacing = horizontalSpacing
    private val innerSpacing = 0

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {

        val itemCount = parent.adapter?.itemCount ?: 0
        val currentItemPos = parent.getChildLayoutPosition(view)

        // add top spacing to rows except for first row
        if (currentItemPos % numberOfRows == 0) {
            outRect.bottom = innerSpacing
        } else {
            outRect.top = innerSpacing
        }

        // add bigger spacing for last column
//        if (currentItemPos > (itemCount - (numberOfRows + 1)) && itemCount > numberOfRows * 2) {
//            outRect.right = outerSpacing
//        } else {
            outRect.right = innerSpacing
//        }

        // add bigger spacing for first column
        if (currentItemPos < numberOfRows) {
            outRect.left = outerSpacing
        } else {
            outRect.left = innerSpacing
        }
    }
}