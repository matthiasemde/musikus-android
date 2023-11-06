package app.musikus.ui.statistics.sessionstatistics

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.musikus.R
import app.musikus.spacing
import app.musikus.utils.DATE_FORMATTER_PATTERN_DAY_OF_MONTH
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
    navigateToStatistics: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                                    SessionStatisticsChartType.PIE -> Icons.Default.PieChart
                                    SessionStatisticsChartType.BAR -> Icons.Default.BarChart
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
                SessionStatisticsHeader(contentUiState.headerUiState)
//                contentUiState.pieChartUiState?.let {
//                    SessionStatisticsPieChart(it)
//                }
//                contentUiState.barChartUiState?.let {
//                    SessionStatisticsBarChart(it)
//                }
            }
        }
    )
}

@Composable
fun SessionStatisticsHeader(
    uiState: SessionStatisticsHeaderUiState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.small),
        horizontalArrangement = SpaceBetween,
        verticalAlignment = CenterVertically,
    ) {
        IconButton(
            onClick = {}
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
        IconButton(onClick = {}) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Forward"
            )
        }
    }
}