/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.musikus.Musikus
import app.musikus.R
import app.musikus.datastore.ThemeSelections
import app.musikus.shared.CommonMenuSelections
import app.musikus.shared.MainMenu
import app.musikus.shared.ThemeMenu
import app.musikus.spacing
import app.musikus.utils.DATE_FORMATTER_PATTERN_DAY_AND_MONTH
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY_SHORT
import app.musikus.utils.epochSecondsToDate
import app.musikus.utils.getDurationString
import app.musikus.viewmodel.MainViewModel
import app.musikus.viewmodel.StatisticsCurrentMonthUiState
import app.musikus.viewmodel.StatisticsGoalCardUiState
import app.musikus.viewmodel.StatisticsPracticeDurationCardUiState
import app.musikus.viewmodel.StatisticsRatingsCardUiState
import app.musikus.viewmodel.StatisticsViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Statistics(
    mainViewModel: MainViewModel,
    statisticsViewModel: StatisticsViewModel = viewModel(),
) {
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val statisticsUiState by statisticsViewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Statistics") },
                scrollBehavior = scrollBehavior,
                actions = {
                    val mainMenuUiState = mainUiState.menuUiState

                    IconButton(onClick = {
                        mainViewModel.showMainMenu()
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = mainMenuUiState.show,
                            onDismissHandler = mainViewModel::hideMainMenu,
                            onSelectionHandler = { commonSelection ->
                                mainViewModel.hideMainMenu()

                                when (commonSelection) {
                                    CommonMenuSelections.APP_INFO -> {}
                                    CommonMenuSelections.THEME -> {
                                        mainViewModel.showThemeSubMenu()
                                    }
                                    CommonMenuSelections.BACKUP -> {
                                        mainViewModel.showExportImportDialog()
                                    }
                                }
                            },
                            uniqueMenuItems = {}
                        )
                        ThemeMenu(
                            expanded = mainMenuUiState.showThemeSubMenu,
                            currentTheme = mainViewModel.activeTheme.collectAsState(initial = ThemeSelections.DAY).value,
                            onDismissHandler = mainViewModel::hideThemeSubMenu,
                            onSelectionHandler = { theme ->
                                mainViewModel.hideThemeSubMenu()
                                mainViewModel.setTheme(theme)
                            }
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            val contentUiState = statisticsUiState.contentUiState
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.large,
                    end = MaterialTheme.spacing.large,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 56.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                contentUiState.currentMonthUiState?.let {
                    item {
                        StatisticsCurrentMonth(it)
                    }
                }
                contentUiState.practiceDurationCardUiState?.let {
                    item {
                        StatisticsPracticeDurationCard(it)
                    }
                }
                contentUiState.goalCardUiState?.let {
                    item {
                        StatisticsGoalCard(it)
                    }
                }
                contentUiState.ratingsCardUiState?.let {
                    item {
                        StatisticsRatingsCard(it)
                    }
                }
            }

            // If there is no data to show, show hint
            if (
                contentUiState.currentMonthUiState == null &&
                contentUiState.practiceDurationCardUiState == null &&
                contentUiState.goalCardUiState == null &&
                contentUiState.ratingsCardUiState == null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.statisticsHint),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable
fun StatisticsCurrentMonth(
    uiState: StatisticsCurrentMonthUiState,
) {
    Column {
        Text(text = "Current month")
        Row(modifier = Modifier.fillMaxWidth()) {
           Column(
               modifier = Modifier
                   .weight(1f)
                   .padding(horizontal = 4.dp),
               horizontalAlignment = CenterHorizontally,
           ) {
               Text(
                   text = getDurationString(
                       uiState.totalPracticeDuration,
                       TIME_FORMAT_HUMAN_PRETTY_SHORT
                   ).toString(),
                   fontSize = 20.sp
               )
               Text(text = "Total duration", fontSize = 12.sp, maxLines = 1)
           }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.average_sign) + " " + getDurationString(
                        uiState.averageDurationPerSession,
                        TIME_FORMAT_HUMAN_PRETTY_SHORT
                    ).toString(),
                    fontSize = 20.sp
                )
                Text(text = "Per session", fontSize = 12.sp, maxLines = 1)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = CenterHorizontally
            ) {
                Text(
                    text = getDurationString(
                        uiState.breakDurationPerHour,
                        TIME_FORMAT_HUMAN_PRETTY_SHORT
                    ).toString(),
                    fontSize = 20.sp
                )
                Text(text = "Break per hour", fontSize = 12.sp, maxLines = 1)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = CenterHorizontally
            ) {
                Text(
                    text =
                        stringResource(R.string.average_sign) + " %.1f".format(
                            uiState.averageRatingPerSession
                        ),
                    fontSize = 20.sp
                )
                Text(text = "Rating", fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun StatisticsPracticeDurationCard(
    uiState: StatisticsPracticeDurationCardUiState,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Practice Time")
                Icon(
                    //                modifier = Modifier.fillMaxHeight(),
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "more",
                )
            }
            Text(text = "Last 7 days")
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
            ) {
                Column {
                    Text(
                        text = getDurationString(
                            uiState.totalPracticeDuration,
                            TIME_FORMAT_HUMAN_PRETTY
                        ).toString()
                    )
                    Text(text = "Total")
                }

                Spacer(modifier = Modifier.weight(2f))

                if (uiState.lastSevenDayPracticeDuration.isEmpty()) {
                    Text(text = "No data")
                } else {
                    Row(
                        modifier = Modifier
                            .height(80.dp)
                            .weight(4f)
                    ) {
                        val maxDuration = uiState.lastSevenDayPracticeDuration.maxOf { it.duration }

                        uiState.lastSevenDayPracticeDuration.forEachIndexed { index, (day, duration) ->
                            val animatedColumnHeight by animateFloatAsState(
                                targetValue = if (maxDuration == 0) 0f else (duration.toFloat() / maxDuration),
                                animationSpec = tween(durationMillis = 1500),
                                label = "day-animation-$index"
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = CenterHorizontally,
                            ) {
                                Column(
                                   modifier= Modifier.weight(1f),
                                   verticalArrangement = Arrangement.Bottom
                                ) {
                                    if(animatedColumnHeight > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(animatedColumnHeight)
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = 2.dp,
                                                        topEnd = 2.dp
                                                    )
                                                )
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = day,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                )
                            }

                            if (index < uiState.lastSevenDayPracticeDuration.size - 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
fun StatisticsGoalCard(
    uiState: StatisticsGoalCardUiState
) {
    ElevatedCard(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Your Goals")
                Icon(
                    //                modifier = Modifier.fillMaxHeight(),
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "more",
                )
            }
            Text(
                text = "Last 5 expired goals"
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
            ) {
                Column {
                    Text(
                        text = "" +
                                "${uiState.lastGoals.filter { it.progress > it.goal.instance.target }.size}" +
                                "/" +
                                "${uiState.lastGoals.size}",
                    )
                    Text(text = "Achieved")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    uiState.lastGoals.forEach { (goal, progress) ->
                        Column(horizontalAlignment = CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(35.dp),
                                progress = progress.toFloat() / goal.instance.target.toFloat(),
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                            Text(
                                text = epochSecondsToDate(goal.instance.let {
                                    it.startTimestamp + it.periodInSeconds
                                }).format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_AND_MONTH)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsRatingsCard(
    uiState: StatisticsRatingsCardUiState
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(MaterialTheme.spacing.medium)
        ) {
            Text(text = "Ratings")
            Text(text = "Your session ratings")
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            val labelTextStyle = MaterialTheme.typography.labelSmall
            val starCharacter = stringResource(R.string.star_sign)
            val colors = Musikus.getLibraryItemColors(LocalContext.current)
            val textMeasurer = rememberTextMeasurer()
            val numOfRatingsToAngleFactor = uiState.numOfRatingsFromOneToFive
                .sum()
                .takeUnless { it == 0 }
                ?.let { 360f / it }
                ?: 0f

            val targetAngles = uiState.numOfRatingsFromOneToFive.map { numOfRatings ->
                numOfRatings * numOfRatingsToAngleFactor
            }

            val animatedAngles = targetAngles.mapIndexed { index, angle ->
                animateFloatAsState(
                    targetValue = angle,
                    animationSpec = tween(durationMillis = 1500),
                    label = "rating-animation-$index"
                )
            }

            val labelAlpha by animateFloatAsState(
                targetValue = if (animatedAngles.sumOf { it.value.toDouble() } < 270f) 0f else 1f,
                animationSpec = tween(durationMillis = 600),
                label = "label-fade-animation"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                var startAngle = 270f

                val pieChartSize = 0.7f * size.height
                val pieChartCenter = Offset(
                    x = size.width / 2,
                    y = size.height / 2
                )
                val pieChartTopLeft = pieChartCenter - Offset(
                    x = pieChartSize / 2,
                    y = pieChartSize / 2
                )
                animatedAngles.zip(uiState.numOfRatingsFromOneToFive).forEachIndexed { index, (angleState, numRatings) ->
                    if (numRatings == 0) return@forEachIndexed
                    val angle = angleState.value

                    val halfSweepEdgePoint = Math.toRadians((startAngle + angle / 2).toDouble()).let {
                        Offset(
                            x = (pieChartSize / 2) * kotlin.math.cos(it).toFloat(),
                            y = (pieChartSize / 2) * kotlin.math.sin(it).toFloat()
                        )
                    }

                    /** pie piece */
                    drawArc(
                        color = Color(colors[index]),
                        startAngle = startAngle,
                        sweepAngle = angle,
                        useCenter = true,
                        topLeft = pieChartTopLeft + halfSweepEdgePoint * 0.025f,
                        size = Size(pieChartSize, pieChartSize),
                        style = Fill
                    )

                    val lineCornerPoint =  pieChartCenter + halfSweepEdgePoint * 1.2f
                    val labelOnRightSide = (startAngle + angle / 2) < 270f + 180f && (startAngle + angle / 2) > 270f

                    startAngle += angle

                    if (labelAlpha == 0f) return@forEachIndexed

                    /** angled line */
                    drawLine(
                        color = Color(colors[index]).copy(alpha = labelAlpha),
                        start = pieChartCenter + halfSweepEdgePoint,
                        end = lineCornerPoint,
                        strokeWidth = 1.dp.toPx()
                    )


                    val lineEndPoint = lineCornerPoint + Offset(
                        x = 24.dp.toPx() * if (labelOnRightSide) 1 else -1,
                        y = 0f
                    )

                    /** horizontal line */
                    drawLine(
                        color = Color(colors[index]).copy(alpha = labelAlpha),
                        start = pieChartCenter + halfSweepEdgePoint * 1.2f,
                        end = lineEndPoint,
                        strokeWidth = 1.dp.toPx()
                    )

                    drawText(
                        textMeasurer,
                        text = (1..(index + 1))
                            .joinToString("") { starCharacter }
                            .plus(" · $numRatings"),
                        topLeft = lineEndPoint + Offset(
                            x = (8.dp.toPx() + (index * 2.dp.toPx())) * if (labelOnRightSide) 0.7f else -4.8f,
                            y = -8.dp.toPx()
                        ),
                        style = labelTextStyle.copy(
                            color = labelTextStyle.color.copy(alpha = 0.7f * labelAlpha),
                        ),
                    )
                }
            }
        }
    }
}