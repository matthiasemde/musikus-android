/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Matthias Emde
 */

package app.musikus.core.presentation.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

suspend fun LazyListState.smoothlyScrollToItem(index: Int) {
    // The short delay improves the feel of the animation.
    delay(1000.milliseconds)

    val isItemVisible = layoutInfo.visibleItemsInfo.any { info ->
        info.index == index
    }

    val viewPortSize = when(layoutInfo.orientation) {
        Orientation.Vertical -> layoutInfo.viewportSize.height
        Orientation.Horizontal -> layoutInfo.viewportSize.width
    }

    val visibleItemsTotalSize = layoutInfo.visibleItemsInfo.sumOf { it.size }

    val targetItemInfo = layoutInfo.visibleItemsInfo.first { it.index == index }

    val isItemFullyVisivble = targetItemInfo.offset in 0..(layoutInfo.viewportSize.height - targetItemInfo.size)

    // Only scroll if the item is not already visible.
    if (layoutInfo.visibleItemsInfo.indexOfFirst { info ->
            info.index == index
        }.let { index -> index < 1 || index > layoutInfo.visibleItemsInfo.size - 3 }
    ) {
        animateScrollToItem(index = index)
    }
}
