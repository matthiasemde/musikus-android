/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.statistics.goalstatistics

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.musikus.Musikus
import app.musikus.R
import app.musikus.ui.statistics.sessionstatistics.ScaleLineData
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.getDurationString
import app.musikus.viewmodel.GoalStatisticsBarChartUiState
import kotlinx.coroutines.launch

@Composable
fun GoalStatisticsBarChart(
    uiState: GoalStatisticsBarChartUiState
) {
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val checkMark = ImageVector.vectorResource(id = R.drawable.ic_check_small_round)
    val checkMarkPainter = rememberVectorPainter(image = checkMark)
    val checkMarkSize = 20.dp

    val columnThickness = 16.dp

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceColorLowerContrast = Color.LightGray
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val libraryColors = Musikus.getLibraryItemColors(LocalContext.current).map {
        Color(it)
    }

    val labelTextStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    val dashedLineEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    val scaleLines = remember { mutableMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    val chartMaxDuration = remember(uiState.data) {
        uiState.data.maxByOrNull { (_, duration) -> duration }?.second ?: 0
    }

    val newScaleLines = remember(chartMaxDuration) {
        when {
            chartMaxDuration > 2 * 60 * 60 -> {
                val hours = chartMaxDuration / 3600
                listOf(
                    (hours + 1) * 3600,
                    (hours + 1) * 1800 // * 3600 / 2
                )
            }

            chartMaxDuration > 1800 -> {
                val halfHours = chartMaxDuration / 1800
                listOf(
                    (halfHours + 1) * 1800,
                    (halfHours + 1) * 900 // * 1800 / 2
                )
            }

            chartMaxDuration > 600 -> {
                val tensOfMinutes = chartMaxDuration / 600
                listOf(
                    (tensOfMinutes + 1) * 600,
                    (tensOfMinutes + 1) * 300 // * 600 / 2
                )
            }

            else -> {
                listOf(
                    10 * 60,
                    5 * 60,
                )
            }
        }
    }

    val scaleLinesWithAnimatedOpacity = remember(newScaleLines + uiState.target) {

        (newScaleLines + scaleLines.keys)
            .distinct()
            .filter {
                it.toFloat() !in ((uiState.target * 0.8)..(uiState.target * 1.2))
            }
            .plus(uiState.target)
            .onEach { scaleLine ->
                val targetOpacity = if (scaleLine in (newScaleLines + uiState.target)) 1f else 0f

                val animatedOpacity = scaleLines[scaleLine] ?: Animatable(0f).also {
                    scaleLines[scaleLine] = it
                }

                scope.launch {
                    val animationResult = animatedOpacity.animateTo(
                        targetValue = targetOpacity,
                        animationSpec = tween(
                            delayMillis = 250,
                            durationMillis = 250),
                    )

                    if (animationResult.endReason == AnimationEndReason.Finished && targetOpacity == 0f) {
                        scaleLines.remove(scaleLine)
                    }
                }
            }
    }.mapNotNull { scaleLine ->
        scaleLines[scaleLine]?.let { animatedAlpha ->
            ScaleLineData(
                label = textMeasurer.measure(
                    getDurationString(scaleLine, TIME_FORMAT_HUMAN_PRETTY).toString(),
                    labelTextStyle.copy(
                        color =
                            (if (scaleLine == uiState.target) primaryColor
                            else onSurfaceColor).copy(
                                alpha = animatedAlpha.asState().value
                            )
                    )
                ),
                duration = scaleLine.toFloat(),
                color =
                    (if (scaleLine == uiState.target) primaryColor
                    else onSurfaceColorLowerContrast).copy(
                        alpha = animatedAlpha.asState().value
                    ),
            )
        }
    }

    val bars = remember { (1..7).map { Animatable(0f) } }

    val labelsWithAnimatedDurations = remember(uiState.data) {
        uiState.data.zip(bars).onEach {(pair, bar) ->
            val (_, duration) = pair

            scope.launch {
                bar.animateTo(
                    duration.toFloat(),
                    animationSpec = tween(durationMillis = 1000)
                )
            }
        }.map { (pair, bar) ->
            val (label, _) = pair
            textMeasurer.measure(label, labelTextStyle) to bar.asState()
        }
    }

    val animatedCheckmarkAlphas = labelsWithAnimatedDurations.mapIndexed { barIndex, (_, animatedDuration) ->
        animateFloatAsState(
            targetValue = if(
                uiState.data[barIndex].second >= uiState.target &&
                animatedDuration.value >= uiState.data[barIndex].second.toFloat() * 0.8
            ) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "bar-chart-check-mark-animation-$barIndex"
        )
    }

    val animatedMaxDuration by animateFloatAsState(
        targetValue = newScaleLines.max().toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "bar-chart-max-duration-animation",
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val paddingLeft = 20.dp
        val paddingRight = 48.dp
        val paddingTop = 12.dp
        val columnThicknessInPx = columnThickness.toPx()

        val checkMarkSizeInPx = Size(checkMarkSize.toPx(), checkMarkSize.toPx())

        val chartWidth = size.width - paddingLeft.toPx() - paddingRight.toPx()
        val spacingInPx = (chartWidth - (columnThicknessInPx * 7)) / 7

        val yZero = 32.dp.toPx()
        val columnYOffset = 0.dp.toPx()
        val yMax = (size.height - yZero - columnYOffset) - paddingTop.toPx()

        /** Print Scale Lines */

        /** Print Scale Lines */
        scaleLinesWithAnimatedOpacity.forEach { scaleLineData ->
            Log.d("bar chart", "scale line: $scaleLineData")
            val lineHeight = (size.height - yZero) - (yMax * (scaleLineData.duration / animatedMaxDuration))
            drawLine(
                color = scaleLineData.color,
                start = Offset(
                    x = paddingLeft.toPx(),
                    y = lineHeight
                ),
                end = Offset(
                    x = size.width - paddingRight.toPx(),
                    y = lineHeight
                ),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashedLineEffect
            )
            drawText(
                textLayoutResult = scaleLineData.label,
                topLeft = Offset(
                    x = paddingLeft.toPx() + chartWidth + 3.dp.toPx(),
                    y = lineHeight - scaleLineData.label.size.height / 2
                )
            )
        }

        labelsWithAnimatedDurations
            .zip(animatedCheckmarkAlphas)
            .forEachIndexed { barIndex, (pair, animatedCheckmarkAlpha) ->
            val (measuredLabel, animatedDuration) = pair
            val leftEdge =
                paddingLeft.toPx() +
                        spacingInPx / 2 +
                        barIndex * (columnThicknessInPx + spacingInPx)
            val rightEdge = leftEdge + columnThicknessInPx

            val animatedBarHeight =
                if (animatedMaxDuration == 0f) 0f
                else (animatedDuration.value / animatedMaxDuration) * yMax
//                            animatedOpenCloseScaler

            val bottomEdge = size.height - (yZero + columnYOffset)
            val topEdge = bottomEdge - animatedBarHeight


            // draw the label and the x axis tick
            drawText(
                textLayoutResult = measuredLabel,
                topLeft = Offset(
                    x = leftEdge + columnThicknessInPx / 2 - measuredLabel.size.width / 2,
                    y = size.height - yZero + 12.dp.toPx()
                )
            )

            drawLine(
                color = onSurfaceColorLowerContrast,
                start = Offset(
                    x = leftEdge + columnThicknessInPx / 2,
                    y = size.height - yZero
                ),
                end = Offset(
                    x = leftEdge + columnThicknessInPx / 2,
                    y = size.height - yZero + 5.dp.toPx()
                ),
                strokeWidth = 2.dp.toPx(),
            )

            // draw the bar
            drawRect(
                color = primaryColor,
                topLeft = Offset(
                    x = leftEdge,
                    y = topEdge
                ),
                size = Size(
                    width = columnThickness.toPx(),
                    height = animatedBarHeight
                )
            )

            // draw the check mark if goal is completed
            if (animatedCheckmarkAlpha.value > 0f) {
                translate(
                    left = leftEdge + columnThicknessInPx / 2 - checkMarkSizeInPx.width / 2,
                    top = topEdge + checkMarkSizeInPx.height / 2 - 12.dp.toPx()
                ) {
                    with(checkMarkPainter) {
                        draw(
                            size = checkMarkSizeInPx,
                            alpha = animatedCheckmarkAlpha.value,
                            colorFilter = ColorFilter.tint(onPrimaryColor)
                        )
                    }
                }
            }

            // clip shape for rounded corners
            if (animatedBarHeight > 0f) {
                drawPath(
                    color = surfaceColor,
                    path = Path().apply {
                        moveTo(leftEdge - 1, topEdge - 1)
                        arcTo(
                            rect = Rect(
                                left = leftEdge - 1,
                                top = topEdge - 1,
                                right = leftEdge + 8.dp.toPx() + 1,
                                bottom = topEdge + 8.dp.toPx() + 1,
                            ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        arcTo(
                            rect = Rect(
                                left = rightEdge - 8.dp.toPx() - 1,
                                top = topEdge - 1,
                                right = rightEdge + 1,
                                bottom = topEdge + 8.dp.toPx() + 1,
                            ),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(rightEdge + 1, topEdge - 1)
                        close()
                    },
                    style = Fill
                )
            }

        }
        drawLine(
            color = onSurfaceColorLowerContrast,
            start = Offset(
                x = paddingLeft.toPx(),
                y = size.height - yZero
            ),
            end = Offset(
                x = size.width - paddingRight.toPx(),
                y = size.height - yZero
            ),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
