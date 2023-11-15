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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.musikus.Musikus
import app.musikus.database.daos.LibraryItem
import app.musikus.datastore.sorted
import app.musikus.viewmodel.SessionStatisticsPieChartUiState
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun SessionStatisticsPieChart(
    uiState: SessionStatisticsPieChartUiState
) {

    val (
        libraryItemToDuration,
        itemSortMode,
        itemSortDirection
    ) = uiState.chartData

    val scope = rememberCoroutineScope()

    val textMeasurer = rememberTextMeasurer()

    val labelTextStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    val absoluteStartAngle = 180f // left side of the circle
    val strokeThickness = 64.dp
    val spacerThickness = 4.dp

    val surfaceColor = MaterialTheme.colorScheme.surface
    val libraryColors = Musikus.getLibraryItemColors(LocalContext.current).map {
        Color(it).copy(alpha = 0.8f)
    }

    val noData =
        libraryItemToDuration.isEmpty() ||
                libraryItemToDuration.values.all { it == 0 }

    val animatedOpenCloseScaler by animateFloatAsState(
        targetValue = if (noData) 0f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "pie-chart-open-close-scaler-animation",
    )

    val itemsToMeasuredLabels = remember(libraryItemToDuration) {
        val totalDuration = libraryItemToDuration.values.sum().toFloat()
        libraryItemToDuration.mapValues { (_, duration) ->
            (duration / totalDuration * 100f).let {
                if ( it.isNaN() || it < 5f) ""
                else it.toInt().toString() + "%"
            }.let {
                textMeasurer.measure(it, labelTextStyle)
            }
        }
    }

    val segments = remember {
        mutableMapOf<LibraryItem, Animatable<Float, AnimationVector1D>>()
    }

    val sortedItemsWithAnimatedDuration = remember(libraryItemToDuration) {
        (segments.keys + libraryItemToDuration.keys)
            .distinct()
            .sorted(
                itemSortMode,
                itemSortDirection
            )
            .onEach { item ->
                val duration = libraryItemToDuration[item] ?: 0

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
            }.mapNotNull { item ->
                segments[item]?.let {
                    item to it.asState()
                }
            }
    }

    val animatedAccumulatedDurations = sortedItemsWithAnimatedDuration.runningFold(
        initial = 0f,
        operation = { start, (_, duration) ->
            start + duration.value
        }
    )

    val animatedTotalAccumulatedDuration = animatedAccumulatedDurations.last()

    val sortedItemsWithAnimatedStartAndSweepAngle = sortedItemsWithAnimatedDuration
        // running fold is always one larger than original list
        .zip(animatedAccumulatedDurations.dropLast(1))
        .map { (pair, accumulatedDuration) ->
            val (item, duration) = pair
            item to if (animatedTotalAccumulatedDuration == 0f) Pair(absoluteStartAngle, 0f) else Pair(
                (
                        (accumulatedDuration / animatedTotalAccumulatedDuration) *
                                180f * animatedOpenCloseScaler
                        ) + absoluteStartAngle,
                (duration.value / animatedTotalAccumulatedDuration) * 180f * animatedOpenCloseScaler
            )
        }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val strokeThicknessInPx = strokeThickness.toPx()
        val spacerThicknessInPx = spacerThickness.toPx()

        val pieChartRadius = 0.9f * java.lang.Float.min(size.height, size.width / 2)
        val pieChartCenter = Offset(
            x = size.width / 2,
            y = size.height - (size.height - pieChartRadius) / 2
        )
        val pieChartTopLeft = pieChartCenter - Offset(
            x = pieChartRadius,
            y = pieChartRadius
        )

        sortedItemsWithAnimatedStartAndSweepAngle
            .forEach { (item, pair) ->
                val (startAngle, sweepAngle) = pair
                if (sweepAngle == 0f) return@forEach

                val halfSweepCenterPoint = Math.toRadians((startAngle + sweepAngle / 2).toDouble()).let {
                    Offset(
                        x = cos(it).toFloat(),
                        y = sin(it).toFloat()
                    ) * (pieChartRadius - strokeThicknessInPx / 2)
                }

                val startSpacerLine = Math.toRadians(startAngle.toDouble()).let {
                    Offset(
                        x = cos(it).toFloat(),
                        y = sin(it).toFloat()
                    )
                }.let {
                    Pair(it * pieChartRadius, it * (pieChartRadius - strokeThicknessInPx))
                }

                val endSpacerLine = Math.toRadians((startAngle + sweepAngle).toDouble()).let {
                    Offset(
                        x = cos(it).toFloat(),
                        y = sin(it).toFloat()
                    )
                }.let {
                    Pair(it * pieChartRadius, it * (pieChartRadius - strokeThicknessInPx))
                }

                drawArc(
                    color = libraryColors[item.colorIndex],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    topLeft = pieChartTopLeft,
                    size = Size(pieChartRadius * 2, pieChartRadius * 2),
                    useCenter = true,
                    style = Fill
                )

                drawLine(
                    color = surfaceColor,
                    start = pieChartCenter + startSpacerLine.first,
                    end = pieChartCenter + startSpacerLine.second,
                    strokeWidth = spacerThicknessInPx
                )

                drawLine(
                    color = surfaceColor,
                    start = pieChartCenter + endSpacerLine.first,
                    end = pieChartCenter + endSpacerLine.second,
                    strokeWidth = spacerThicknessInPx
                )

                if (sweepAngle > 10f) {
                    itemsToMeasuredLabels[item]?.let {  measuredLabel ->
                        drawText(
                            textLayoutResult = measuredLabel,
                            topLeft = pieChartCenter + halfSweepCenterPoint - Offset(
                                x = measuredLabel.size.width.toFloat() / 2,
                                y = measuredLabel.size.height.toFloat() / 2
                            ),
                        )
                    }
                }
            }

        drawCircle(
            color = surfaceColor,
            radius = (pieChartRadius - strokeThicknessInPx),
            center = pieChartCenter,
        )
    }
}