package app.musikus.ui.statistics.sessionstatistics

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.musikus.Musikus
import app.musikus.R
import app.musikus.database.daos.LibraryItem
import app.musikus.datastore.sort
import app.musikus.datastore.sorted
import app.musikus.shared.simpleVerticalScrollbar
import app.musikus.spacing
import app.musikus.utils.DATE_FORMATTER_PATTERN_DAY_AND_MONTH
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.getDurationString
import app.musikus.viewmodel.SessionStatisticsBarChartUiState
import app.musikus.viewmodel.SessionStatisticsChartType
import app.musikus.viewmodel.SessionStatisticsHeaderUiState
import app.musikus.viewmodel.SessionStatisticsPieChartUiState
import app.musikus.viewmodel.SessionStatisticsTab
import app.musikus.viewmodel.SessionStatisticsViewModel
import kotlinx.coroutines.launch
import java.lang.Float.min
import java.time.format.DateTimeFormatter
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionStatistics(
    viewModel: SessionStatisticsViewModel = viewModel(),
    navigateToStatistics: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            val topBarUiState = uiState.topBarUiState
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.session_statistics)) },
                navigationIcon = {
                    IconButton(onClick = navigateToStatistics) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::onChartTypeButtonClicked
                    ) {
                        Crossfade(
                            targetState = topBarUiState.chartType,
                            label = "statistics-chart-icon-animation"
                        ) { chartType ->
                            Icon(
                                imageVector = when (chartType) {
                                    SessionStatisticsChartType.PIE -> Icons.Default.BarChart
                                    SessionStatisticsChartType.BAR -> Icons.Default.PieChart
                                },
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        },
        content = { contentPadding ->
            val contentUiState = uiState.contentUiState
            Column {
                Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))
                PrimaryTabRow(
                    selectedTabIndex = contentUiState.selectedTab.ordinal,
                    tabs = {
                        SessionStatisticsTab.entries.forEach { tab ->
                            Tab(
                                selected = tab == contentUiState.selectedTab,
                                onClick = { viewModel.onTabSelected(tab) },
                                text = {
                                    Text(
                                        text = when (tab) {
                                            SessionStatisticsTab.DAYS -> stringResource(id = R.string.days)
                                            SessionStatisticsTab.WEEKS -> stringResource(id = R.string.weeks)
                                            SessionStatisticsTab.MONTHS -> stringResource(id = R.string.months)
                                        }.replaceFirstChar { it.uppercase() }
                                    )
                                }
                            )
                        }
                    }
                )
                SessionStatisticsHeader(
                    uiState = contentUiState.headerUiState,
                    seekForward = viewModel::onSeekForwardClicked,
                    seekBackward = viewModel::onSeekBackwardClicked
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                ) {
                    contentUiState.pieChartUiState?.let { SessionStatisticsPieChart(it) }
                    contentUiState.barChartUiState?.let { SessionStatisticsBarChart(it) }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                HorizontalDivider()
                SessionStatisticsLibraryItemSelector(
                    contentUiState.libraryItemsWithSelection,
                    viewModel::onLibraryItemCheckboxClicked
                )
            }
        }
    )
}

@Composable
fun SessionStatisticsHeader(
    uiState: SessionStatisticsHeaderUiState,
    seekForward: () -> Unit = {},
    seekBackward: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.small),
        horizontalArrangement = SpaceBetween,
        verticalAlignment = CenterVertically,
    ) {
        IconButton(
            onClick = seekBackward,
            enabled = uiState.seekBackwardEnabled
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back"
            )
        }
        Column(
            verticalArrangement = Center,
            horizontalAlignment = CenterHorizontally
        ) {
            Text(
                text = uiState.timeFrame.let { (start, end) ->
                    listOf(start, end.minusSeconds(1))
                }.joinToString(" - ") {
                    it.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_AND_MONTH))
                }, // TODO calculate nice strings
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Total " + getDurationString(uiState.totalPracticeDuration, TIME_FORMAT_HUMAN_PRETTY),
                style = MaterialTheme.typography.titleSmall
            )
        }
        IconButton(
            onClick = seekForward,
            enabled = uiState.seekForwardEnabled
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Forward"
            )
        }
    }
}

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
    val strokeThickness = 150f
    val spacerThickness = 8f

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
        val pieChartRadius = 0.9f * min(size.height, size.width / 2)
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
                    x = kotlin.math.cos(it).toFloat(),
                    y = kotlin.math.sin(it).toFloat()
                ) * (pieChartRadius - strokeThickness / 2)
            }

            val startSpacerLine = Math.toRadians(startAngle.toDouble()).let {
                Offset(
                    x = kotlin.math.cos(it).toFloat(),
                    y = kotlin.math.sin(it).toFloat()
                )
            }.let {
                Pair(it * pieChartRadius, it * (pieChartRadius - strokeThickness))
            }

            val endSpacerLine = Math.toRadians((startAngle + sweepAngle).toDouble()).let {
                Offset(
                    x = kotlin.math.cos(it).toFloat(),
                    y = kotlin.math.sin(it).toFloat()
                )
            }.let {
                Pair(it * pieChartRadius, it * (pieChartRadius - strokeThickness))
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
                strokeWidth = spacerThickness
            )

            drawLine(
                color = surfaceColor,
                start = pieChartCenter + endSpacerLine.first,
                end = pieChartCenter + endSpacerLine.second,
                strokeWidth = spacerThickness
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
            radius = (pieChartRadius - strokeThickness),
            center = pieChartCenter,
        )
    }
}

@Composable
fun SessionStatisticsBarChart(
    uiState: SessionStatisticsBarChartUiState
) {
    val (barData, chartMaxDuration, itemSortMode, itemSortDirection) = uiState.chartData

    val columnThickness = 16.dp

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
//    val primaryColor = MaterialTheme.colorScheme.primary
    val libraryColors = Musikus.getLibraryItemColors(LocalContext.current).map {
        Color(it)
    }

    val noData = barData.all { barDatum ->
        barDatum.libraryItemsToDuration.isEmpty() ||
        barDatum.libraryItemsToDuration.values.all { it == 0 }
    }

    val animatedOpenCloseScaler by animateFloatAsState(
        targetValue = if (noData) 0f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "bar-chart-open-close-scaler-animation",
    )

    val segmentsForBars = remember { barData.map {
        mutableMapOf<LibraryItem, Animatable<Float, AnimationVector1D>>()
    }}

    val sortedItemsForBars = remember { barData.map {
        mutableListOf<LibraryItem>()
    }}

    barData.zip(segmentsForBars).zip(sortedItemsForBars)
        .forEach { (pair, sortedItems) ->
        val (barDatum, segments) = pair

        LaunchedEffect(barDatum) {
            (segments.keys + barDatum.libraryItemsToDuration.keys)
            .distinct()
            .forEach { item ->
                val duration = barDatum.libraryItemsToDuration[item] ?: 0

                val segment = segments[item] ?: Animatable(0f).also {
                    segments[item] = it
                    sortedItems.add(item)
                    sortedItems.sort(itemSortMode, itemSortDirection)
                }
                launch {
                    val animationResult = segment.animateTo(
                        duration.toFloat(),
                        animationSpec = tween(durationMillis = 1000),
                    )
                    if (animationResult.endReason == AnimationEndReason.Finished && duration == 0) {
                        segments.remove(item)
                        sortedItems.remove(item)
                    }
                }
            }
        }
    }

    val sortedItemsWithAnimatedDurationForBars = sortedItemsForBars.zip(segmentsForBars)
        .map { (items, segments) ->
        items.mapNotNull { item ->
            segments[item]?.let {
                item to it.asState().value
            }
        }
    }

    val animatedChartMaxDuration by animateFloatAsState(
        targetValue = chartMaxDuration.toFloat(),
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

    val textMeasurer = rememberTextMeasurer()
    val labelTextStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    val labelsForBars = remember(barData.map { it.label }) {
        barData.map { it.label }
    }

    val measuredLabelsForBars = remember(labelsForBars) {
        labelsForBars.map { textMeasurer.measure(it, labelTextStyle) }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val columnThicknessInPx = columnThickness.toPx()
        val spacingInPx = (size.width - (columnThicknessInPx * 7)) / 8

        val yZero = 32.dp.toPx()
        val columnYOffset = 0.dp.toPx()
        val columnHeight = size.height - yZero - columnYOffset

        sortedItemsWithAnimatedDurationForBars
            .zip(animatedBarMaxDurationForBars)
            .zip(measuredLabelsForBars)
            .forEachIndexed { barIndex, (pair, measuredLabel) ->
            val (sortedItemsWithAnimatedDuration, animatedBarMaxDuration) = pair
            val leftEdge = barIndex * (columnThicknessInPx + spacingInPx) + spacingInPx
            val rightEdge = leftEdge + columnThicknessInPx

            val animatedAccumulatedDurations = sortedItemsWithAnimatedDuration.runningFold (
                initial = 0f,
                operation = { start, (_, duration) ->
                    start + duration
                }
            )

            val animatedTotalAccumulatedDuration = animatedAccumulatedDurations.last()

            val animatedBarHeight =
                if (animatedChartMaxDuration == 0f) 0f
                else
                    (
                        (animatedBarMaxDuration / animatedChartMaxDuration) *
                        (columnHeight) *
                        animatedOpenCloseScaler
                    )

            val animatedStartAndSegmentHeights = sortedItemsWithAnimatedDuration
                .zip(animatedAccumulatedDurations.dropLast(1))
                .map { (pair, accumulatedDuration) ->
                val (item, duration) = pair
                item to if (animatedTotalAccumulatedDuration == 0f) Pair(0f, 0f) else Pair(
                    accumulatedDuration / animatedTotalAccumulatedDuration * animatedBarHeight + (yZero + columnYOffset),
                    duration / animatedTotalAccumulatedDuration * animatedBarHeight
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
                    strokeWidth = 2f
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
                    strokeWidth = 2f
                )
            }

            if (animatedOpenCloseScaler == 0f) return@Canvas

            drawPath(
                color = surfaceColor,
                path = Path().apply {
                    moveTo(leftEdge, 0f)
                    arcTo(
                        rect = Rect(
                            left = leftEdge,
                            top = size.height - animatedBarHeight - (yZero + columnYOffset),
                            right = leftEdge + 8.dp.toPx(),
                            bottom = size.height - animatedBarHeight + 8.dp.toPx() - (yZero + columnYOffset),
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    arcTo(
                        rect = Rect(
                            left = rightEdge - 8.dp.toPx(),
                            top = size.height - animatedBarHeight - (yZero + columnYOffset),
                            right = rightEdge,
                            bottom = size.height - animatedBarHeight + 8.dp.toPx() - (yZero + columnYOffset),
                        ),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    lineTo(rightEdge, 0f)
                    close()
                },
                style = Fill
            )

            drawText(
                textLayoutResult = measuredLabel,
                topLeft = Offset(
                    x = leftEdge + columnThicknessInPx / 2 - measuredLabel.size.width / 2,
                    y = size.height - yZero + 12.dp.toPx()
                )
            )

            drawLine(
                color = onSurfaceColor,
                start = Offset(
                    x = leftEdge + columnThicknessInPx / 2,
                    y = size.height - yZero
                ),
                end = Offset(
                    x = leftEdge + columnThicknessInPx / 2,
                    y = size.height - yZero + 6.dp.toPx()
                ),
                strokeWidth = 2.dp.toPx()
            )
        }

        drawLine(
            color = onSurfaceColor,
            start = Offset(
                x = 24.dp.toPx(),
                y = size.height - yZero
            ),
            end = Offset(
                x = size.width - 24.dp.toPx(),
                y = size.height - yZero
            ),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun SessionStatisticsLibraryItemSelector(
    libraryItemsWithSelections: List<Pair<LibraryItem, Boolean>>,
    onLibraryItemCheckboxClicked: (LibraryItem) -> Unit = {}
) {
    val scrollState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.simpleVerticalScrollbar(
            scrollState,
            width = 5.dp,
            verticalPadding = MaterialTheme.spacing.extraSmall
        ),
        state = scrollState,
    ) {
        items(
            items = libraryItemsWithSelections,
            key = { (item) -> item.id }
        ) {(item, checked) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onLibraryItemCheckboxClicked(item) }),
                verticalAlignment = CenterVertically
            ) {
                val libraryColor = Color(Musikus.getLibraryItemColors(LocalContext.current)[item.colorIndex])
                Checkbox(
                    checked = checked,
                    colors = CheckboxDefaults.colors(
                        checkedColor = libraryColor,
                        uncheckedColor = libraryColor,
                    ),
                    onCheckedChange = { onLibraryItemCheckboxClicked(item) }
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.name
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            }
        }
    }
}
