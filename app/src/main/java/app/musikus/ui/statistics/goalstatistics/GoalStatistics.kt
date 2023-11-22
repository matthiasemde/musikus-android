/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.statistics.goalstatistics

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.entities.GoalType
import app.musikus.shared.simpleVerticalScrollbar
import app.musikus.spacing
import app.musikus.ui.statistics.sessionstatistics.TimeframeSelectionHeader
import app.musikus.utils.asString
import app.musikus.viewmodel.GoalStatisticsGoalSelectorUiState
import app.musikus.viewmodel.GoalStatisticsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalStatistics(
    viewModel: GoalStatisticsViewModel = viewModel(),
    navigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.goal_statistics)) },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        content = { contentPadding ->
            val contentUiState = uiState.contentUiState
            Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
                Column(modifier = Modifier.height(256.dp)) {
                    contentUiState.headerUiState?.let { TimeframeSelectionHeader(
                        timeframe = it.timeframe,
                        subtitle = it.successRate?.let { (succeeded, total) ->
                            "$succeeded out of $total"
                        } ?: "",
                        seekBackwardEnabled = it.seekBackwardEnabled,
                        seekForwardEnabled = it.seekForwardEnabled,
                        seekForwards = viewModel::seekForwards,
                        seekBackwards = viewModel::seekBackwards,
                    ) }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    contentUiState.barChartUiState?.let {
                        GoalStatisticsBarChart(it)
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                HorizontalDivider()
                contentUiState.goalSelectorUiState?.let { GoalStatisticsGoalSelector(
                    it,
                    onGoalSelected = viewModel::onGoalSelected
                ) }
            }
        }
    )
}

@Composable
fun GoalStatisticsGoalSelector(
    uiState: GoalStatisticsGoalSelectorUiState,
    onGoalSelected: (GoalDescriptionWithInstancesAndLibraryItems) -> Unit = {}
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
            items = uiState.goalsInfo,
            key = { goalInfo -> goalInfo.goal.description.id }
        ) {goalInfo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onGoalSelected(goalInfo.goal) }),
                verticalAlignment = CenterVertically
            ) {
                Log.d("GoalStatistics", "GoalStatisticsGoalSelector: ${goalInfo}")
                val color = if(goalInfo.goal.description.type == GoalType.ITEM_SPECIFIC) {
                    Color(Musikus.getLibraryItemColors(LocalContext.current)[goalInfo.goal.libraryItems.first().colorIndex])
                } else MaterialTheme.colorScheme.primary
                RadioButton(
                    selected = goalInfo.selected,
                    onClick = { onGoalSelected(goalInfo.goal) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = color,
                        unselectedColor = color,
                    )
                )
                Column {
                    // Title
                    Text(
                        text = goalInfo.goal.title.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    // Subtitle
                    Text(
                        text = goalInfo.goal.subtitle?.asString() ?: "No data available", // TODO find a better solution for no instances
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
