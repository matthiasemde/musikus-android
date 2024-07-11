/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.statistics.presentation.sessionstatistics

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.R
import app.musikus.library.data.daos.LibraryItem
import app.musikus.ui.components.conditional
import app.musikus.ui.components.simpleVerticalScrollbar
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.theme.spacing
import app.musikus.utils.Timeframe
import app.musikus.utils.musikusFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionStatistics(
    viewModel: SessionStatisticsViewModel = hiltViewModel(),
    navigateUp: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            val topBarUiState = uiState.topBarUiState
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.session_statistics)) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
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
                    seekForwards = viewModel::onSeekForwardClicked,
                    seekBackwards = viewModel::onSeekBackwardClicked
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
                    contentUiState.libraryItemsWithSelectionAndDuration,
                    viewModel::onLibraryItemCheckboxClicked
                )
            }
        }
    )
}

@Composable
fun SessionStatisticsHeader(
    uiState: SessionStatisticsHeaderUiState,
    seekForwards: () -> Unit = {},
    seekBackwards: () -> Unit = {}
) = TimeframeSelectionHeader(
    timeframe = uiState.timeframe,
    subtitle = "Total " + getDurationString(uiState.totalPracticeDuration, DurationFormat.HUMAN_PRETTY),
    seekBackwardEnabled = uiState.seekBackwardEnabled,
    seekForwardEnabled = uiState.seekForwardEnabled,
    seekForwards = seekForwards,
    seekBackwards = seekBackwards
)




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionStatisticsLibraryItemSelector(
    libraryItemsWithSelectionAndDuration: List<Triple<LibraryItem, Boolean, String>>,
    onLibraryItemCheckboxClicked: (LibraryItem) -> Unit = {}
) {
    val scrollState = rememberLazyListState()
    val showScrollbar = scrollState.canScrollBackward || scrollState.canScrollForward
    LazyColumn(
        modifier = Modifier.conditional(showScrollbar) { simpleVerticalScrollbar(
            scrollState,
            width = 5.dp,
            verticalPadding = MaterialTheme.spacing.extraSmall
        ) },
        state = scrollState,
    ) {
        items(
            items = libraryItemsWithSelectionAndDuration,
            key = { (item) -> item.id }
        ) {(item, checked, duration) ->
            Row(
                modifier = Modifier
                    .animateItemPlacement()
                    .fillMaxWidth()
                    .clickable(onClick = { onLibraryItemCheckboxClicked(item) }),
                verticalAlignment = CenterVertically
            ) {
                val libraryItemColor = libraryItemColors[item.colorIndex]
                Checkbox(
                    checked = checked,
                    colors = CheckboxDefaults.colors(
                        checkedColor = libraryItemColor,
                        uncheckedColor = libraryItemColor,
                    ),
                    onCheckedChange = { onLibraryItemCheckboxClicked(item) }
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(MaterialTheme.spacing.large))
                Text(
                    text = duration,
                    style = MaterialTheme.typography.titleSmall
                )
                val spacerWidth = animateDpAsState(
                    targetValue =
                        if(showScrollbar) MaterialTheme.spacing.large
                        else MaterialTheme.spacing.medium,
                    label = "animatedSpacerWidth"
                ).value
                Spacer(
                    modifier = Modifier.width(spacerWidth)
                )
            }
        }
    }
}

@Composable
fun TimeframeSelectionHeader(
    timeframe: Timeframe,
    subtitle: String,
    seekBackwardEnabled: Boolean,
    seekForwardEnabled: Boolean,
    seekForwards: () -> Unit,
    seekBackwards: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.small),
        horizontalArrangement = SpaceBetween,
        verticalAlignment = CenterVertically,
    ) {
        IconButton(
            onClick = seekBackwards,
            enabled = seekBackwardEnabled
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
                text = timeframe.let { (start, end) ->
                    start to end.minusSeconds(1)
                }.musikusFormat(),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall
            )
        }
        IconButton(
            onClick = seekForwards,
            enabled = seekForwardEnabled
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Forward"
            )
        }
    }
}