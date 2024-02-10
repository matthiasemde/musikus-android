/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt


@Composable
fun Waveform(
    modifier: Modifier = Modifier,
    rawRecording: FloatArray?,
    playBackMarker : Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onClick: (Float) -> Unit
) {

    val loudnessScaling = 0.0001f
    val minProportionalBarHeight = 0.02f
    // the rounded Caps of the bars extend beyond the bar height,
    // so we shrink them down to where they fit again
    val maxProportionalBarHeight = 0.9f

    val numberOfBars = 30

    val barHeightAnimatables = remember {
        (0 until numberOfBars).map { Animatable(0f) }
    }

    LaunchedEffect(key1 = rawRecording) {

        if (rawRecording == null) {
            barHeightAnimatables.forEachIndexed { i, animatable -> launch {
                animatable.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(200, delayMillis = i * 5)
                )
            } }
            return@LaunchedEffect
        }

        withContext(Dispatchers.Default) {

            val numberOfSamplesPerBar = rawRecording.size.floorDiv(numberOfBars - 1)

            // Calculate the loudness of the recording as RMS
            val squaredSamples = FloatArray(rawRecording.size) { i ->
                rawRecording[i].pow(2)
            }

            val rms = DoubleArray(numberOfBars) { i ->
                val start = i * numberOfSamplesPerBar
                val end = if (i < numberOfBars - 1) {
                    (i + 1) * numberOfSamplesPerBar
                } else {
                    rawRecording.size
                }
                sqrt(
                    squaredSamples
                        .sliceArray(start until end)
                        .average()
                ) * loudnessScaling
            }
            rms.zip(barHeightAnimatables).forEachIndexed { i, (rms, animatable) ->
                launch {
                    animatable.animateTo(
                        targetValue = rms.toFloat(),
                        animationSpec = tween(
                            durationMillis = 250,
                            delayMillis = i * 15
                        ),
                    )
                }
            }
        }
    }

    val animatedProportionalBarHeights = barHeightAnimatables.map { it.asState().value }

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
            while (barIndex < numberOfBars) {
                val barX = barIndex * (barWidth + barSpacing) + barWidth / 2

                val barHeight =
                    size.height *
                    animatedProportionalBarHeights[barIndex].coerceIn(
                        minimumValue = minProportionalBarHeight,
                        maximumValue = maxProportionalBarHeight
                    )


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
                if(barIndex != (numberOfBars * playBackMarker).toInt()) {
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
