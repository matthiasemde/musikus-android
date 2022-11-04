/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.shared

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
    height: Float = 0.2f,
    width: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
): Modifier {
    return drawWithContent {
        drawContent()
        val totalItemCount = state.layoutInfo.totalItemsCount.toFloat()

        val itemHeight = state.layoutInfo.visibleItemsInfo.first().size

        val missingItems = (totalItemCount * itemHeight - state.layoutInfo.viewportSize.height) / itemHeight

        if(missingItems > 1) {
            val scrollbarHeight = state.layoutInfo.viewportSize.height.toFloat() * height // in pixels
            val scrollPerItem = (state.layoutInfo.viewportSize.height - scrollbarHeight) / missingItems

            val offsetPercentage = state.firstVisibleItemScrollOffset.toFloat() / itemHeight

            val firstIndex = state.firstVisibleItemIndex.toFloat()
            val scrollbarOffsetY = (firstIndex + offsetPercentage) * scrollPerItem

            drawRoundRect(
                cornerRadius = CornerRadius(width.toPx() / 2),
                color = color,
                topLeft = Offset(this.size.width - (width + 5.dp).toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
            )
        }
    }
}