/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.statistics.sessionstatistics

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.graphics.Color
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
import app.musikus.utils.DATE_FORMATTER_PATTERN_DAY_AND_MONTH
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.getDurationString
import app.musikus.viewmodel.SessionStatisticsChartType
import app.musikus.viewmodel.SessionStatisticsHeaderUiState
import app.musikus.viewmodel.SessionStatisticsTab
import app.musikus.viewmodel.SessionStatisticsViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionStatistics(
    viewModel: SessionStatisticsViewModel = viewModel(),
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
