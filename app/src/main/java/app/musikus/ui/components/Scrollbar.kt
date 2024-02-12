/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.components

import androidx.compose.foundation.ScrollState
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
    state: ScrollState,
    viewportHeight: Float,
    width: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    maxSize: Dp = 150.dp,
): Modifier {
    return drawWithContent {
        drawContent()

        val scrollableContentHeight = state.maxValue + viewportHeight

        val percentageOfColumnShownInViewPort = viewportHeight / (scrollableContentHeight)
        val scrollbarHeight = (percentageOfColumnShownInViewPort * viewportHeight).coerceAtMost(maxSize.toPx()) // in pixels

        val scrollbarOffsetY = state.value * (percentageOfColumnShownInViewPort + 1)

        drawRoundRect(
            cornerRadius = CornerRadius(width.toPx() / 2),
            color = color,
            topLeft = Offset(this.size.width - (width + 5.dp).toPx(), scrollbarOffsetY),
            size = Size(width.toPx(), scrollbarHeight),
        )
        drawRoundRect(
            cornerRadius = CornerRadius(width.toPx() / 2),
            color = color.copy(alpha = 0.2f),
            topLeft = Offset(this.size.width - (width + 5.dp).toPx(), state.value.toFloat()),
            size = Size(width.toPx(), viewportHeight),
        )
    }
}

@Composable
fun Modifier.simpleVerticalScrollbar(
    listState: LazyListState,
    width: Dp = 3.dp,
    verticalPadding: Dp = 0.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    maxSize: Dp = 150.dp,
): Modifier {
    return drawWithContent {
        drawContent()
        val totalItemCount = listState.layoutInfo.totalItemsCount.toFloat()
        val trackHeight = listState.layoutInfo.viewportSize.height.toFloat() - 2 * verticalPadding.toPx()

        val averageVisibleItemHeight = listState.layoutInfo.visibleItemsInfo.map {
            it.size
        }.average().toFloat()

        val approximatedTotalItemHeight = totalItemCount * averageVisibleItemHeight

        val percentageShowing = listState.layoutInfo.viewportSize.height / approximatedTotalItemHeight

        val firstVisibleItemHeight =
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 0f
        val firstVisibleItemOffsetPercentage = listState.firstVisibleItemScrollOffset.toFloat() / firstVisibleItemHeight


        val scrollbarHeight = (trackHeight * percentageShowing).coerceAtMost(maxSize.toPx()) // in pixels
        val scrollPerItem = (trackHeight - scrollbarHeight) / (totalItemCount * (1 - percentageShowing)) // in pixels

        if(listState.canScrollBackward || listState.canScrollForward) {
            val firstIndex = listState.firstVisibleItemIndex.toFloat()
            val scrollbarOffsetY = (firstIndex + firstVisibleItemOffsetPercentage) * scrollPerItem

            drawRoundRect(
                cornerRadius = CornerRadius(width.toPx() / 2),
                color = color,
                topLeft = Offset(this.size.width - (width + 5.dp).toPx(), scrollbarOffsetY + verticalPadding.toPx()),
                size = Size(width.toPx(), scrollbarHeight),
            )
            drawRoundRect(
                cornerRadius = CornerRadius(width.toPx() / 2),
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(this.size.width - (width + 5.dp).toPx(), verticalPadding.toPx()),
                size = Size(width.toPx(), trackHeight),
            )
        }
    }
}