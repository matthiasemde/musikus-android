/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.statistics.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.Bottom
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.musikus.R
import app.musikus.core.presentation.HomeUiEvent
import app.musikus.core.presentation.HomeUiEventHandler
import app.musikus.core.presentation.HomeUiState
import app.musikus.core.presentation.Screen
import app.musikus.core.presentation.components.CommonMenuSelections
import app.musikus.core.presentation.components.MainMenu
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.theme.spacing
import app.musikus.statistics.presentation.goalstatistics.GoalStatistics
import app.musikus.statistics.presentation.sessionstatistics.SessionStatistics
import app.musikus.core.domain.DateFormat
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.core.domain.musikusFormat

fun NavGraphBuilder.addStatisticsNavigationGraph(
    navController: NavController,
) {
    composable(
        route = Screen.SessionStatistics.route,
    ) { SessionStatistics(navigateUp = navController::navigateUp) }
    composable(
        route = Screen.GoalStatistics.route,
    ) { GoalStatistics(navigateUp = navController::navigateUp) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Statistics(
    statisticsViewModel: StatisticsViewModel = hiltViewModel(),
    homeUiState: HomeUiState,
    homeEventHandler: HomeUiEventHandler,
    navigateTo: (Screen) -> Unit,
    timeProvider: TimeProvider,
) {
    val statisticsUiState by statisticsViewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Statistics") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = {
                        homeEventHandler(HomeUiEvent.ShowMainMenu)
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = homeUiState.showMainMenu,
                            onDismiss = { homeEventHandler(HomeUiEvent.HideMainMenu) },
                            onSelection = { commonSelection ->
                                homeEventHandler(HomeUiEvent.HideMainMenu)

                                when (commonSelection) {
                                    CommonMenuSelections.SETTINGS -> { navigateTo(Screen.Settings) }
                                }
                            },
                            uniqueMenuItems = {}
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
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
                    StatisticsCurrentMonth(
                        it,
                        timeProvider = timeProvider
                    )
                }
            }
            contentUiState.practiceDurationCardUiState?.let {
                item {
                    StatisticsPracticeDurationCard(it) { navigateTo(Screen.SessionStatistics) }
                }
            }
            contentUiState.goalCardUiState?.let {
                item {
                    StatisticsGoalCard(it) { navigateTo(Screen.GoalStatistics) }
                }
            }
            contentUiState.ratingsCardUiState?.let {
                item {
                    StatisticsRatingsCard(it)
                }
            }
        }

        // Show hint if there is no session data
        if (contentUiState.showHint) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.extraLarge),
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
}

@Composable
fun StatisticsCurrentMonth(
    uiState: StatisticsCurrentMonthUiState,
    timeProvider: TimeProvider
) {

    val labelTextStyle = MaterialTheme.typography.labelSmall
    val statsTextStyle = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.primary,
    )

    val currentMonthStats = listOf(
        "Total duration" to getDurationString(
            uiState.totalPracticeDuration,
            DurationFormat.HUMAN_PRETTY_SHORT
        ),
        "Per session" to AnnotatedString(stringResource(R.string.average_sign) + " ") + getDurationString(
            uiState.averageDurationPerSession,
            DurationFormat.HUMAN_PRETTY_SHORT
        ),
        "Break per hour" to getDurationString(
            uiState.breakDurationPerHour,
            DurationFormat.HUMAN_PRETTY_SHORT
        ),
        "Average rating" to AnnotatedString(stringResource(R.string.average_sign) + " %.1f".format(
            uiState.averageRatingPerSession
        )),
    )

    Column {
        Text(text = "In " + timeProvider.now().musikusFormat(DateFormat.MONTH))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            currentMonthStats.forEach {(label, stat) ->
               Column(
                   modifier = Modifier
                       .weight(1f),
                   horizontalAlignment = CenterHorizontally,
               ) {
                   Text(text = stat, style = statsTextStyle)
                   Text(text = label, style = labelTextStyle)
               }
            }
        }
    }
}

@Composable
fun StatisticsPracticeDurationCard(
    uiState: StatisticsPracticeDurationCardUiState,
    navigateToSessionStatistics: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = navigateToSessionStatistics)
            .padding(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = SpaceBetween,
                verticalAlignment = CenterVertically,
            ) {
                Text(
                    text = "Practice Time",
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "more",
                )
            }
            Text(
                text = "Last 7 days",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.7f
                    ),
                )
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = SpaceBetween,
                verticalAlignment = CenterVertically,
            ) {
                Column {
                    Text(
                        text = getDurationString(
                            uiState.totalPracticeDuration,
                            DurationFormat.HUMAN_PRETTY
                        ).toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.7f
                            )
                        ),
                    )
                }

                Spacer(modifier = Modifier.weight(2f))

                Row(
                    modifier = Modifier
                        .height(80.dp)
                        .weight(4f)
                ) {
                    val maxDurationSeconds = uiState.lastSevenDayPracticeDuration.maxOf { it.duration }.inWholeSeconds

                    uiState.lastSevenDayPracticeDuration.forEachIndexed { index, (day, duration) ->
                        val animatedColumnHeight by animateFloatAsState(
                            targetValue =
                                if (maxDurationSeconds == 0L) 0f
                                else (duration.inWholeSeconds.toFloat() / maxDurationSeconds),
                            animationSpec = tween(
                                durationMillis = 1500,
                                delayMillis = 100 * index
                        ),
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
                               verticalArrangement = Bottom
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
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
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

                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
fun StatisticsGoalCard(
    uiState: StatisticsGoalCardUiState,
    navigateToGoalStatistics: () -> Unit,
) {
    ElevatedCard(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = navigateToGoalStatistics)
            .padding(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = SpaceBetween,
                verticalAlignment = CenterVertically,
            ) {
                Text(
                    text = "Your Goals",
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    //                modifier = Modifier.fillMaxHeight(),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "more",
                )
            }
            Text(
                text = "Last 5 expired goals",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.7f
                    ),
                )
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = CenterVertically,
            ) {
                uiState.successRate?.let { (successful, total) ->
                    Column {
                        Text(
                            text = "" +
                                    "$successful" +
                                    "/" +
                                    "$total",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = "Achieved",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.7f
                                )
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    uiState.lastGoalsDisplayData.forEachIndexed { index, displayData ->
                        Column(horizontalAlignment = CenterHorizontally) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = displayData.progress,
                                animationSpec = tween(
                                    durationMillis = 1500,
                                    delayMillis = 100 * index
                                ),
                                label = "goal-animation-$index"
                            )
                            val color = displayData.color ?: MaterialTheme.colorScheme.primary
                            Box{
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(35.dp),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                )
                                val animatedAlpha by animateFloatAsState(
                                    targetValue = if (animatedProgress == 1f) 1f else 0f,
                                    animationSpec = tween(durationMillis = 500),
                                    label = "goal-icon-animation-$index"
                                )
                                Icon(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(4.dp),
                                    painter = painterResource(id = R.drawable.ic_check_small_round),
                                    contentDescription = null,
                                    tint = color.copy(
                                        alpha = animatedAlpha
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                            Text(
                                text = displayData.label,
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
            .padding(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = "Ratings",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Your session ratings",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.7f
                    ),
                )
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            val labelTextStyle = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            )
            val starCharacter = stringResource(R.string.star_sign)
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
                        color = libraryItemColors[index],
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
                        color = libraryItemColors[index].copy(alpha = labelAlpha),
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
                        color = libraryItemColors[index].copy(alpha = labelAlpha),
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