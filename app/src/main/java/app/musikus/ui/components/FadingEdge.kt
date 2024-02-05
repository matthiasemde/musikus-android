package app.musikus.ui.components

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

import androidx.compose.foundation.gestures.ScrollableState
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
fun Modifier.fadingEdge(scrollState: ScrollableState, vertical: Boolean = true) : Modifier {
    val bckPossible = scrollState.canScrollBackward
    val fwdPossible = scrollState.canScrollForward

    val brush = getBrush(bckPossible, fwdPossible, vertical)
    return getModifiers(brush)
}

private fun Modifier.getModifiers(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

private fun getBrush(top: Boolean, bottom: Boolean, vertical: Boolean): Brush {
    return if (vertical) {
        Brush.verticalGradient(
            0f to if(top) Color.Transparent else Color.Red,
            0.05f to Color.Red,
            0.95f to Color.Red,
            1f to if(bottom) Color.Transparent else Color.Red
        )
    } else {
        Brush.horizontalGradient(
            0f to if(top) Color.Transparent else Color.Red,
            0.05f to Color.Red,
            0.95f to Color.Red,
            1f to if(bottom) Color.Transparent else Color.Red
        )
    }
}