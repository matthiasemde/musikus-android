package app.musikus.ui.components

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer


/**
 * Modifier adding a fading edge to the top and bottom of the list depending on its scroll state.
 * */
fun Modifier.fadingEdge(scrollState: LazyListState) : Modifier {
    val top = scrollState.canScrollBackward
    val bottom = scrollState.canScrollForward

    val brush = getBrush(top, bottom)
    return getModifiers(brush)
}


/** Overload for the scroll state of a scrollable. */
fun Modifier.fadingEdge(scrollState: ScrollState) : Modifier {
    val top = scrollState.canScrollBackward
    val bottom = scrollState.canScrollForward

    val brush = getBrush(top, bottom)
    return getModifiers(brush)
}


private fun Modifier.getModifiers(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

private fun getBrush(top: Boolean, bottom: Boolean): Brush {
    return Brush.verticalGradient(
        0f to if(top) Color.Transparent else Color.Red,
        0.05f to Color.Red,
        0.95f to Color.Red,
        1f to if(bottom) Color.Transparent else Color.Red
    )
}