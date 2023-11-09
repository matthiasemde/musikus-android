package app.musikus.ui.statistics.sessionstatistics

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.musikus.Musikus
import app.musikus.R
import app.musikus.database.daos.LibraryItem
import app.musikus.shared.simpleVerticalScrollbar
import app.musikus.spacing
import app.musikus.utils.DATE_FORMATTER_PATTERN_DAY_OF_MONTH
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.getDurationString
import app.musikus.viewmodel.SessionStatisticsBarChartUiState
import app.musikus.viewmodel.SessionStatisticsChartType
import app.musikus.viewmodel.SessionStatisticsHeaderUiState
import app.musikus.viewmodel.SessionStatisticsPieChartUiState
import app.musikus.viewmodel.SessionStatisticsTab
import app.musikus.viewmodel.SessionStatisticsViewModel
import java.lang.Float.min
import java.time.format.DateTimeFormatter

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
                Crossfade(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    targetState =
                        if (contentUiState.pieChartUiState != null) SessionStatisticsChartType.PIE
                        else if (contentUiState.barChartUiState != null) SessionStatisticsChartType.BAR
                        else null,
                    animationSpec = tween(durationMillis = 300),
                    label = "statistics-chart-crossfade-animation"
                ) { targetChart ->
                    when (targetChart) {
                        SessionStatisticsChartType.PIE ->
                            contentUiState.pieChartUiState?.let {
                                SessionStatisticsPieChart(it)
                            }
                        SessionStatisticsChartType.BAR ->
                            contentUiState.barChartUiState?.let {
                                SessionStatisticsBarChart(it)
                            }
                        null -> Text(text = "No data")
                    }
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
        IconButton(onClick = seekBackward) {
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
                text = uiState.timeFrame.toList().joinToString(" - ") {
                    it.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_OF_MONTH))
                }, // TODO calculate nice strings
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Total " + getDurationString(uiState.totalPracticeDuration, TIME_FORMAT_HUMAN_PRETTY),
                style = MaterialTheme.typography.titleSmall
            )
        }
        IconButton(onClick = seekForward) {
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
    val absoluteStartAngle = 180f // left side of the circle
    val strokeThickness = 150f
    val spacerThickness = 8f

    val surfaceColor = MaterialTheme.colorScheme.surface
    val libraryColors = Musikus.getLibraryItemColors(LocalContext.current).map {
        Color(it)
    }

    val libraryItemsToDuration = uiState.libraryItemsToDuration

    val noData =
        libraryItemsToDuration.isEmpty() ||
        libraryItemsToDuration.values.all { it == 0 }

    val animatedOpenCloseScaler by animateFloatAsState(
        targetValue = if (noData) 0f else 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "pie-chart-open-close-scaler-animation",
    )

    val shownLibraryItems = remember {
        libraryItemsToDuration.map { (item, _) -> item }.toMutableSet()
    }

    val libraryItemsWithAnimatedDuration = (
        shownLibraryItems + libraryItemsToDuration.keys
    ).distinct().map { item ->
        val duration = libraryItemsToDuration[item] ?: 0

        if (item !in shownLibraryItems) {
            shownLibraryItems.add(item)
        }

        item to animateIntAsState(
            targetValue = duration,
            animationSpec = tween(durationMillis = 1000),
            label = "pie-chart-sweep-angle-animation-${item.id}",
        )
    }

    val animatedAccumulatedDurations = libraryItemsWithAnimatedDuration.runningFold(
        initial = 0,
        operation = { start, (_, duration) ->
            start + duration.value
        }
    )

    val animatedMaxDuration = animatedAccumulatedDurations.last()

    val libraryItemsWithAnimatedStartAndSweepAngle = libraryItemsWithAnimatedDuration
        // running fold is always one larger than original list
        .zip(animatedAccumulatedDurations.dropLast(1))
        .map { (pair, accumulatedDuration) ->
            val (item, duration) = pair
            item to if (animatedMaxDuration == 0) Pair(absoluteStartAngle, 0f) else Pair(
                (
                    (accumulatedDuration.toFloat() / animatedMaxDuration) *
                    180f * animatedOpenCloseScaler
                ) + absoluteStartAngle,
                (duration.value.toFloat() / animatedMaxDuration) * 180f * animatedOpenCloseScaler
            )
        }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
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

        libraryItemsWithAnimatedStartAndSweepAngle.forEachIndexed { index, (item, pair) ->
            val (startAngle, sweepAngle) = pair
            if (sweepAngle == 0f) return@forEachIndexed

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

            // draw start spacer for every item except the first one
            if (index > 0) {
                drawLine(
                    color = surfaceColor,
                    start = pieChartCenter + startSpacerLine.first,
                    end = pieChartCenter + startSpacerLine.second,
                    strokeWidth = spacerThickness
                )
            }

            // draw end spacer for every item except the first one
            if (index < libraryItemsWithAnimatedStartAndSweepAngle.size - 1) {
                drawLine(
                    color = surfaceColor,
                    start = pieChartCenter + endSpacerLine.first,
                    end = pieChartCenter + endSpacerLine.second,
                    strokeWidth = spacerThickness
                )
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
    val (barData, maxDuration) = uiState

    val columnThickness = 16.dp

    val surfaceColor = MaterialTheme.colorScheme.surface
    val libraryColors = Musikus.getLibraryItemColors(LocalContext.current).map {
        Color(it)
    }

    val libraryItemsToAnimatedDurationForBars = barData.mapIndexed { barIndex, barDatum ->
        val shownLibraryItemsInBar = remember {
            mutableListOf<LibraryItem>()
        }

        (shownLibraryItemsInBar + barDatum.libraryItemsToDuration.keys)
            .distinct()
//            .also { items ->
//                if(barIndex ==5) Log.d("session-statistics", "shownLibraryItemsInBar: ${items.map {it.name}}")
//            }
            .associateWith { item ->
                val duration = barDatum.libraryItemsToDuration[item] ?: 0

                if (item !in shownLibraryItemsInBar) {
                    shownLibraryItemsInBar.add(item)
                }

                animateFloatAsState(
                    targetValue = duration.toFloat(),
                    animationSpec = tween(durationMillis = 1000),
                    label = "bar-chart-column-${barIndex}-animation-${item.id}",
                )
            }
    }

    val (
        animatedAccumulatedDurationsForBars,
        animatedTotalDurationForBars
    ) = libraryItemsToAnimatedDurationForBars.map {
        it.values.runningFold(
            initial = 0f,
            operation = { start, duration ->
                start + duration.value
            }
        )
    }.map{ Pair(it.dropLast(1), it.last()) }.unzip()


    val animatedMaxDuration = animatedTotalDurationForBars.max()

//                barData.map {barDatum -> barDatum.libraryItemsToDuration.map { (item, _) -> item }.toMutableSet() }
//            }

//            val shownLibraryItemsInBar = shownLibraryItemsInChart[barIndex]

//            .filter { (_, animatedDuration) ->
//                animatedDuration.value != 0
//            }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val columnThicknessInPx = columnThickness.toPx()
        val spacingInPx = (size.width - (columnThicknessInPx * 7)) / 8

        libraryItemsToAnimatedDurationForBars
            .zip(animatedAccumulatedDurationsForBars)
            .forEachIndexed { barIndex, (
                libraryItemsToAnimatedDurations,
                animatedAccumulatedDurations
            ) ->
                val leftEdge = barIndex * (columnThicknessInPx + spacingInPx) + spacingInPx
                val rightEdge = leftEdge + columnThicknessInPx

                val libraryItemsWithAnimatedStartAndSegmentHeight =
                    libraryItemsToAnimatedDurations.entries
                        .zip(animatedAccumulatedDurations)
                        .map { (pair, accumulatedDuration) ->
                            val (item, duration) = pair
                            item to if (animatedMaxDuration == 0f) Pair(0f, 0f) else Pair(
                                (accumulatedDuration / animatedMaxDuration) * size.height,
                                (duration.value / animatedMaxDuration) * size.height
                            )
                        }

                libraryItemsWithAnimatedStartAndSegmentHeight.forEach {(item, pair) ->
                    val (startHeight, segmentHeight) = pair

                    if (segmentHeight == 0f) return@forEach
                    drawRect(
                        color = libraryColors[item.colorIndex],
                        topLeft = Offset(
                            x = leftEdge,
                            y = size.height - startHeight - segmentHeight
                        ),
                        size = Size(
                            width = columnThickness.toPx(),
                            height = segmentHeight
                        )
                    )

                    drawLine(
                        color = surfaceColor,
                        start = Offset(
                            x = leftEdge,
                            y = size.height - startHeight
                        ),
                        end = Offset(
                            x = rightEdge,
                            y = size.height - startHeight
                        ),
                        strokeWidth = 2f
                    )

                    drawLine(
                        color = surfaceColor,
                        start = Offset(
                            x = leftEdge,
                            y = size.height - startHeight - segmentHeight
                        ),
                        end = Offset(
                            x = rightEdge,
                            y = size.height - startHeight - segmentHeight
                        ),
                        strokeWidth = 2f
                    )
                }
            }
//                val libraryItemsToAnimatedStartAndSegmentHeight =
//                .map {  ->
//                // running fold is always one larger than original list
//                .zip(animatedAccumulatedDurations.dropLast(1))
//                .map { (pair, accumulatedDuration) ->
//                    val (item, duration) = pair
//                    item to if (animatedTotalDuration == 0) Pair(0f, 0f) else Pair(
//                        (accumulatedDuration.toFloat() / animatedTotalDuration) * animatedColumnHeight,
//                        (duration.value.toFloat() / animatedTotalDuration) * animatedColumnHeight
//                    )
//                }
//            }
//            }
//                libraryItemsWithAnimatedStartAndSegmentHeight.forEachIndexed bars@{ index, (item, pair) ->
//                    val (startHeight, segmentHeight) = pair
//
//                    if (segmentHeight == 0f) return@forEachIndexed
//
//                }
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
            key = { (item, ) -> item.id }
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
