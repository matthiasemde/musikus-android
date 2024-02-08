/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.pow


@Composable
fun Waveform(
    modifier: Modifier = Modifier,
    rawRecording: FloatArray,
    playBackMarker : Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onClick: (Float) -> Unit
) {

    val minAmplitudeThreshold = 10.0f
    val minProportionalBarHeight = 0.05f
    val minMaxAmplitude = 0.1f
    val exponent = 0.4f
    val numberOfBars = 30

    val normalizedBars by remember(rawRecording) {
        derivedStateOf {
            if(rawRecording.isEmpty()) return@derivedStateOf emptyList()

            val chunkedAbsoluteBars = rawRecording
                .asSequence()
                .map { abs(it) }
                .chunked(rawRecording.size / numberOfBars)
                .map { chunk ->
                    chunk.average().toFloat()
                }
                .map { bar ->
                    (bar - minAmplitudeThreshold).coerceAtLeast(0f) // filter out background noise
                }
                .map { bar ->
                    bar.pow(exponent)
                }

//            val minAmplitude = chunkedAbsoluteBars.min() // .coerceAtMost(log(0.1f, base))
            val maxAmplitude = chunkedAbsoluteBars.max().coerceAtLeast(0.01f)

            Log.d("Waveform", "min/maxAmplitude: $maxAmplitude")

            val normalizedBars = chunkedAbsoluteBars
                .map { (it / maxAmplitude) }
                .toList()
                .also { bars ->
                    Log.d("Waveform", "normalizedBars: ${bars}")
                }

            val startEndMask = listOf(0.6f, 0.8f)

            return@derivedStateOf normalizedBars.mapIndexed { index, bar ->
                if (index < startEndMask.size) {
                    bar * startEndMask[index]
                } else if (index > normalizedBars.size - startEndMask.size) {
                    bar * startEndMask[normalizedBars.size - index - 1]
                } else {
                    bar
                }
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures { offset -> onClick(offset.x / size.width) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd
                ) { change, _ ->
                    change.consume()
                    onDrag(change.position.x / size.width)
                }
            }
    ) {
        val barSpacing = size.width / (numberOfBars * 3 + 2)
        val barWidth = 2 * barSpacing

        var barIndex = 0

        repeat(2) { i ->
            while (barIndex < normalizedBars.size) {
                val barX = barIndex * (barWidth + barSpacing) + barWidth / 2

                val barHeight =
                    size.height *
                    normalizedBars[barIndex].coerceAtLeast(minProportionalBarHeight) *
                    0.9f // the rounded Caps of the bars extend beyond the bar height, so we shrink them down to where they fit again

                val distFromTop = (size.height - barHeight) / 2

                val drawBar = {
                    drawLine(
                        color = primaryColor.copy(alpha = if(i == 0) 1f else 0.5f),
                        start = Offset(barX, distFromTop),
                        end = Offset(barX, distFromTop + barHeight),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }

                // find the bar that the playBackMarker is currently on
                if(barIndex != (normalizedBars.size * playBackMarker).toInt()) {
                    drawBar()
                } else {
                    // if we are in "already played" section (i==0),
                    // clip the bar to the right of the playBackMarker and break the loop
                    if(i == 0) {
                        clipRect(
                            left = 0f,
                            top = 0f,
                            right = size.width * playBackMarker,
                            bottom = size.height
                        ) {
                            drawBar()
                        }
                        break
                    }
                    // if we are in "not yet played" section (i!=0),
                    // clip the bar to the left of the playBackMarker
                    clipRect(
                        left = size.width * playBackMarker,
                        top = 0f,
                        right = size.width,
                        bottom = size.height
                    ) {
                        drawBar()
                    }
                }

                barIndex++
            }
        }
    }
}
