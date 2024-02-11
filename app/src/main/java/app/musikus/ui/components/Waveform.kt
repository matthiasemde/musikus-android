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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt


@Composable
fun Waveform(
    modifier: Modifier = Modifier,
    rawRecording: ShortArray?,
    playBackMarker : Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onClick: (Float) -> Unit
) {

    val loudnessScaling = 0.00015f
    val minProportionalBarHeight = 0.02f
    // the rounded Caps of the bars extend beyond the bar height,
    // so we shrink them down to where they fit again
    val maxProportionalBarHeight = 0.9f

    val numberOfBars = 30

    val barHeightAnimatables = remember {
        (0 until numberOfBars).map { Animatable(minProportionalBarHeight) }
    }

    LaunchedEffect(key1 = rawRecording) {

        if (rawRecording == null || rawRecording.isEmpty()) {
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
            val squaredSamples = IntArray(rawRecording.size) { i ->
                rawRecording[i] * rawRecording[i]
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
                        targetValue = rms.toFloat().coerceIn(
                            minimumValue = minProportionalBarHeight,
                            maximumValue = maxProportionalBarHeight
                        ),
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


        val clipPath = Path().apply {
            animatedProportionalBarHeights.forEachIndexed { index, proportionalBarHeight ->
                val x = index * (barWidth + barSpacing)
                val barHeight = proportionalBarHeight * size.height

                val topY = (size.height - barHeight) / 2
                val bottomY = size.height - (size.height - barHeight) / 2

                moveTo(x, bottomY)
                arcTo(
                    rect = Rect(
                        left = x,
                        top = topY - barWidth / 2,
                        right = x + barWidth,
                        bottom = topY + barWidth / 2
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                arcTo(
                    rect = Rect(
                        left = x,
                        top = bottomY - barWidth / 2,
                        right = x + barWidth,
                        bottom = bottomY + barWidth / 2
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }
        }

        clipPath(
            path = clipPath
        ) {
            drawRect(
                color = primaryColor.copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = size
            )

            drawRect(
                color = primaryColor.copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width * playBackMarker, size.height)
            )
        }
    }
}
