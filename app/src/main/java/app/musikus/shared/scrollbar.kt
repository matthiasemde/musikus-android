/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.shared

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// source : https://stackoverflow.com/questions/66341823/jetpack-compose-scrollbars/68056586#68056586

@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
): Modifier {
    return drawWithContent {
        drawContent()
        val totalItemCount = state.layoutInfo.totalItemsCount.toFloat()

        var visibleItemHeight = 0f
        state.layoutInfo.visibleItemsInfo.forEach { visibleItemHeight += it.size.toFloat() }
        val avgVisibleItemHeight = visibleItemHeight / state.layoutInfo.visibleItemsInfo.size

        val approximatedTotalItemHeight = totalItemCount * avgVisibleItemHeight

        val percentageShowing = state.layoutInfo.viewportSize.height / approximatedTotalItemHeight

        val firstVisibleItemHeight =
            state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 0f
        val firstVisibleItemOffsetPercentage = state.firstVisibleItemScrollOffset.toFloat() / firstVisibleItemHeight

        val scrollbarHeight = state.layoutInfo.viewportSize.height.toFloat() * percentageShowing // in pixels
        val scrollPerItem = (state.layoutInfo.viewportSize.height - scrollbarHeight) / (totalItemCount * (1 - percentageShowing)) // in pixels

        if(state.layoutInfo.viewportSize.height - scrollbarHeight > 2) {
            val firstIndex = state.firstVisibleItemIndex.toFloat()
            val scrollbarOffsetY = (firstIndex + firstVisibleItemOffsetPercentage) * scrollPerItem

            drawRoundRect(
                cornerRadius = CornerRadius(width.toPx() / 2),
                color = color,
                topLeft = Offset(this.size.width - (width + 5.dp).toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
            )
            drawRoundRect(
                cornerRadius = CornerRadius(width.toPx() / 2),
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(this.size.width - (width + 5.dp).toPx(), 0f),
                size = Size(width.toPx(), state.layoutInfo.viewportSize.height.toFloat()),
            )
        }
    }
}