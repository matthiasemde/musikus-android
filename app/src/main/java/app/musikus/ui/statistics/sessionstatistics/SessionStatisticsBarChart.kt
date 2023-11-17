/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.statistics.sessionstatistics

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.musikus.Musikus
import app.musikus.database.daos.LibraryItem
import app.musikus.datastore.sorted
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.getDurationString
import app.musikus.viewmodel.SessionStatisticsBarChartUiState
import kotlinx.coroutines.launch

data class ScaleLineData (
    val label: TextLayoutResult,
    val duration: Float,
    val color: Color,
    val target: Boolean = false,
)
@Composable
fun SessionStatisticsBarChart(
    uiState: SessionStatisticsBarChartUiState
) {
    val (barData, chartMaxDuration, itemSortMode, itemSortDirection) = uiState.chartData

    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    val columnThickness = 16.dp
    val spacerThickness = 1.dp

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceColorLowerContrast = Color.LightGray
//    val primaryColor = MaterialTheme.colorScheme.primary
    val libraryColors = Musikus.getLibraryItemColors(LocalContext.current).map {
        Color(it)
    }

    val labelTextStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    val dashedLineEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    val noData = barData.all { barDatum ->
        barDatum.libraryItemsToDuration.isEmpty() ||
                barDatum.libraryItemsToDuration.values.all { it == 0 }
    }

    val animatedOpenCloseScaler by animateFloatAsState(
        targetValue = if (noData) 0f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "bar-chart-open-close-scaler-animation",
    )

    val labelsForBars = barData.map { it.label }
    val measuredLabelsForBars = remember(labelsForBars) {
        labelsForBars.map { label ->
            textMeasurer.measure(label, labelTextStyle)
        }
    }

    val scaleLines = remember { mutableMapOf<Int, Animatable<Float, AnimationVector1D>>() }

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

    val scaleLinesWithAnimatedOpacity = remember(newScaleLines) {

        (newScaleLines + scaleLines.keys)
            .distinct()
            .onEach { scaleLine ->
                val targetOpacity = if (scaleLine in newScaleLines) 1f else 0f

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
                        color = onSurfaceColor.copy(
                            alpha = animatedAlpha.asState().value
                        )
                    )
                ),
                duration = scaleLine.toFloat(),
                color = onSurfaceColorLowerContrast.copy(
                    alpha = animatedAlpha.asState().value
                ),
            )
        }
    }

    val sortedItemsWithAnimatedDurationForBars = barData.map { barDatum ->
        val libraryItemsToDuration = barDatum.libraryItemsToDuration

        val segments = remember {
            mutableMapOf<LibraryItem, Animatable<Float, AnimationVector1D>>()
        }

        remember(libraryItemsToDuration) {
            (segments.keys + libraryItemsToDuration.keys)
                .distinct()
                .sorted(
                    itemSortMode,
                    itemSortDirection
                )
                .onEach { item ->
                    val duration = libraryItemsToDuration[item] ?: 0

                    val segment = segments[item] ?: Animatable(0f).also {
                        segments[item] = it
                    }

                    scope.launch {
                        val animationResult = segment.animateTo(
                            duration.toFloat(),
                            animationSpec = tween(durationMillis = 1000),
                        )

                        if (animationResult.endReason == AnimationEndReason.Finished && duration == 0) {
                            segments.remove(item)
                        }
                    }
                }
        }.mapNotNull { item ->
            segments[item]?.let {
                item to it.asState()
            }
        }
    }

    val animatedChartMaxDuration by animateFloatAsState(
        targetValue = newScaleLines.max().toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "bar-chart-max-duration-animation",
    )

    val animatedBarMaxDurationForBars = barData.mapIndexed { barIndex, barDatum ->
        animateFloatAsState(
            targetValue =
            if (animatedChartMaxDuration == 0f) 0f
            else barDatum.totalDuration.toFloat(),
            animationSpec = tween(durationMillis = 1000),
            label = "bar-chart-max-duration-animation-${barIndex}",
        ).value
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val paddingLeft = 20.dp
        val paddingRight = 48.dp
        val paddingTop = 12.dp
        val columnThicknessInPx = columnThickness.toPx()

        val chartWidth = size.width - paddingLeft.toPx() - paddingRight.toPx()
        val spacingInPx = (chartWidth - (columnThicknessInPx * 7)) / 7

        val yZero = 32.dp.toPx()
        val columnYOffset = 0.dp.toPx()
        val yMax = (size.height - yZero - columnYOffset) - paddingTop.toPx()

        /** Print Scale Lines */
        scaleLinesWithAnimatedOpacity.forEach { (label, duration, color) ->
            val lineHeight = (size.height - yZero) - (yMax * (duration / animatedChartMaxDuration))
            drawLine(
                color = color,
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
                textLayoutResult = label,
                topLeft = Offset(
                    x = paddingLeft.toPx() + chartWidth + 3.dp.toPx(),
                    y = lineHeight - label.size.height / 2
                )
            )
        }

        sortedItemsWithAnimatedDurationForBars
            .zip(animatedBarMaxDurationForBars)
            .zip(measuredLabelsForBars)
            .forEachIndexed { barIndex, (pair, measuredLabel) ->
                val (sortedItemsWithAnimatedDuration, animatedBarMaxDuration) = pair
                val leftEdge =
                    paddingLeft.toPx() +
                    spacingInPx / 2 +
                    barIndex * (columnThicknessInPx + spacingInPx)
                val rightEdge = leftEdge + columnThicknessInPx

                val animatedAccumulatedDurations = sortedItemsWithAnimatedDuration.runningFold (
                    initial = 0f,
                    operation = { start, (_, duration) ->
                        start + duration.value
                    }
                )

                val animatedTotalAccumulatedDuration = animatedAccumulatedDurations.last()

                val animatedBarHeight =
                    if (animatedChartMaxDuration == 0f) 0f
                    else
                        (
                            (animatedBarMaxDuration / animatedChartMaxDuration) *
                                (yMax) *
                                animatedOpenCloseScaler
                        )

                val animatedStartAndSegmentHeights = sortedItemsWithAnimatedDuration
                    .zip(animatedAccumulatedDurations.dropLast(1))
                    .map { (pair, accumulatedDuration) ->
                        val (item, duration) = pair
                        item to if (animatedTotalAccumulatedDuration == 0f) Pair(0f, 0f) else Pair(
                            accumulatedDuration / animatedTotalAccumulatedDuration * animatedBarHeight + (yZero + columnYOffset),
                            duration.value / animatedTotalAccumulatedDuration * animatedBarHeight
                        )
                    }

                animatedStartAndSegmentHeights.forEach { (item, pair) ->
                    val (animatedStartHeight, animatedSegmentHeight) = pair

                    val bottomEdge = size.height - animatedStartHeight
                    val topEdge = bottomEdge - animatedSegmentHeight

                    if (animatedSegmentHeight == 0f) return@forEach
                    drawRect(
                        color = libraryColors[item.colorIndex],
                        topLeft = Offset(
                            x = leftEdge,
                            y = topEdge
                        ),
                        size = Size(
                            width = columnThickness.toPx(),
                            height = animatedSegmentHeight
                        )
                    )

                    drawLine(
                        color = surfaceColor,
                        start = Offset(
                            x = leftEdge,
                            y = bottomEdge
                        ),
                        end = Offset(
                            x = rightEdge,
                            y = bottomEdge
                        ),
                        strokeWidth = spacerThickness.toPx()
                    )

                    drawLine(
                        color = surfaceColor,
                        start = Offset(
                            x = leftEdge,
                            y = topEdge
                        ),
                        end = Offset(
                            x = rightEdge,
                            y = topEdge
                        ),
                        strokeWidth = spacerThickness.toPx()
                    )
                }

                // clip shape for rounded corners
                if (animatedBarHeight > 0f) {
                    val topEdge = size.height - animatedBarHeight - (yZero + columnYOffset) + spacerThickness.toPx() / 2
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