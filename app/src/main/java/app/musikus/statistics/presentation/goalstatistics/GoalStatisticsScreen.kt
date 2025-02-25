/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2025 Matthias Emde
 */

package app.musikus.statistics.presentation.goalstatistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign.Companion.End
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.core.presentation.MusikusTopBar
import app.musikus.core.presentation.components.conditional
import app.musikus.core.presentation.components.simpleVerticalScrollbar
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.UiText
import app.musikus.core.presentation.utils.asAnnotatedString
import app.musikus.statistics.presentation.sessionstatistics.TimeframeSelectionHeader
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalStatistics(
    viewModel: GoalStatisticsViewModel = hiltViewModel(),
    navigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            MusikusTopBar(
                isTopLevel = false,
                title = UiText.StringResource(R.string.statistics_goal_statistics_title),
                scrollBehavior = scrollBehavior,
                navigateUp = navigateUp
            )
        },
        content = { contentPadding ->
            val contentUiState = uiState.contentUiState
            Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
                Column(modifier = Modifier.height(256.dp)) {
                    contentUiState.headerUiState?.let {
                        TimeframeSelectionHeader(
                            timeframe = it.timeframe,
                            subtitle = it.successRate?.let { (succeeded, total) ->
                                stringResource(
                                    id = R.string.statistics_goal_statistics_achieved_goals,
                                    succeeded,
                                    total
                                )
                            } ?: "",
                            seekBackwardEnabled = it.seekBackwardEnabled,
                            seekForwardEnabled = it.seekForwardEnabled,
                            seekForwards = viewModel::seekForwards,
                            seekBackwards = viewModel::seekBackwards,
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    contentUiState.barChartUiState?.let {
                        GoalStatisticsBarChart(it)
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                HorizontalDivider()
                contentUiState.goalSelectorUiState?.let {
                    GoalStatisticsGoalSelector(
                        it,
                        onGoalSelected = viewModel::onGoalSelected
                    )
                }
            }
        }
    )
}

@Composable
fun GoalStatisticsGoalSelector(
    uiState: GoalStatisticsGoalSelectorUiState,
    onGoalSelected: (UUID) -> Unit = {}
) {
    val scrollState = rememberLazyListState()
    val showScrollbar = scrollState.canScrollBackward || scrollState.canScrollForward
    LazyColumn(
        modifier = Modifier.conditional(showScrollbar) {
            simpleVerticalScrollbar(
                scrollState,
                width = 5.dp,
                verticalPadding = MaterialTheme.spacing.extraSmall
            )
        },
        state = scrollState,
    ) {
        items(
            items = uiState.goalsInfo,
            key = { goalInfo -> goalInfo.goalId }
        ) { goalInfo ->
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Max)
                    .fillMaxWidth()
                    .clickable(onClick = { onGoalSelected(goalInfo.goalId) }),
                verticalAlignment = CenterVertically
            ) {
                val color = goalInfo.uniqueColor ?: MaterialTheme.colorScheme.primary
                RadioButton(
                    selected = goalInfo.selected,
                    onClick = { onGoalSelected(goalInfo.goalId) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = color,
                        unselectedColor = color,
                    )
                )
//                VerticalDivider(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .padding(
//                            vertical = 10.dp,
//                            horizontal = MaterialTheme.spacing.extraSmall
//                        ),
//                    thickness = 2.dp,
//                )
//                Icon(
//                    modifier = Modifier
//                        .padding(horizontal = MaterialTheme.spacing.small)
//                        .size(20.dp),
//                    imageVector =
//                        if(goalInfo.goal.description.repeat) Icons.Rounded.Repeat
//                        else Icons.Filled.LocalFireDepartment,
//                    contentDescription = if(goalInfo.goal.description.repeat)
//                        "Regular goal" else "One shot goal",
//                    tint = color
//                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = MaterialTheme.spacing.small)
                ) {
                    // Title
                    Text(
                        text = goalInfo.title.asAnnotatedString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    // Subtitle
                    Text(
                        text = goalInfo.subtitle?.asAnnotatedString() ?: "No data available", // TODO find a better solution for no instances
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = MaterialTheme.spacing.medium)
                        .widthIn(min = 65.dp)
                        .width(IntrinsicSize.Min)
                ) {
                    AnimatedVisibility(
                        visible = goalInfo.successRate != null,
                        enter = fadeIn(),
                    ) {
                        goalInfo.successRate?.let { (successful, _) ->
                            Text(
                                text = successful.toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(vertical = MaterialTheme.spacing.extraSmall)
                            .fillMaxWidth()
                            .height(4.dp)
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = goalInfo.successRate?.let { (successful, total) ->
                                (successful.toFloat() / total).coerceAtMost(1f)
                            } ?: 0f,
                            label = "animate-success-rate",
                            animationSpec = tween(1500)
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.matchParentSize(),
                            color = color,
                            drawStopIndicator = {},
                        )
                    }
                    AnimatedVisibility(
                        visible = goalInfo.successRate != null,
                        modifier = Modifier.fillMaxWidth(),
                        enter = fadeIn(),
                    ) {
                        goalInfo.successRate?.let { (successful, total) ->
                            Text(
                                text = (total - successful).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = End
                            )
                        }
                    }
                }
                Spacer(
                    modifier = Modifier
                        .width(
                            if (showScrollbar) {
                                MaterialTheme.spacing.large
                            } else {
                                MaterialTheme.spacing.medium
                            }
                        )
                        .animateContentSize()
                )
            }
        }
    }
}
