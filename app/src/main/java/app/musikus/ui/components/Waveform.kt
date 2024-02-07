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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.media3.session.MediaController
import app.musikus.ui.theme.spacing
import kotlin.math.abs
import kotlin.math.log


@Composable
fun Waveform(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    rawRecording: FloatArray,
    mediaController: MediaController,
) {

    val minAmplitudeThreshold = 10.0f
    val minProportionalBarHeight = 0.05f
    val minMaxAmplitude = 0.1f
    val base = 10f
    val numberOfBars = 30

    LaunchedEffect(key1 = rawRecording) {
        Log.d("Waveform", "rawRecording: ${rawRecording.size}")
    }

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
                    log(bar, base = base).coerceAtLeast(0f)
                }

//            val minAmplitude = chunkedAbsoluteBars.min() // .coerceAtMost(log(0.1f, base))
            val maxAmplitude = chunkedAbsoluteBars.max() // .coerceAtLeast(log(0.1f, base))

            Log.d("Waveform", "min/maxAmplitude: $maxAmplitude")

            chunkedAbsoluteBars
                .map { (it / maxAmplitude) }
                .toList()
                .also { bars ->
                    Log.d("Waveform", "normalizedBars: ${bars}")
                }

        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(MaterialTheme.spacing.extraSmall)
    ) {
        val barSpacing = size.width / (numberOfBars * 3 + 2)
        val barWidth = 2 * barSpacing

        normalizedBars.forEachIndexed { index, normalizedBarAmplitude ->
            val barX = index * (barWidth + barSpacing)

            val barHeight = size.height * normalizedBarAmplitude.coerceAtLeast(minProportionalBarHeight)
            val distFromTop = (size.height - barHeight) / 2

            drawLine(
                color = primaryColor,
                start = Offset(barX, distFromTop),
                end = Offset(barX, distFromTop + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
