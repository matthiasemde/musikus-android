/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.statistics.presentation.sessionstatistics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.R
import app.musikus.core.data.EnumWithIcon
import app.musikus.core.data.EnumWithLabel
import app.musikus.core.data.SectionWithLibraryItem
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.SortDirection
import app.musikus.core.domain.SortInfo
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.Timeframe
import app.musikus.core.domain.musikusFormat
import app.musikus.core.domain.specificDay
import app.musikus.core.domain.specificMonth
import app.musikus.core.domain.specificWeek
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.UiIcon
import app.musikus.core.presentation.utils.UiText
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.library.data.LibraryItemSortMode
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.sorted
import app.musikus.library.domain.usecase.LibraryUseCases
import app.musikus.sessions.domain.usecase.SessionsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Ui state data classes
 */

enum class SessionStatisticsTab : EnumWithLabel {
    DAYS {
        override val label = UiText.StringResource(R.string.statistics_session_statistics_tab_days)
    },
    WEEKS {
        override val label = UiText.StringResource(R.string.statistics_session_statistics_tab_weeks)
    },
    MONTHS {
        override val label = UiText.StringResource(R.string.statistics_session_statistics_tab_months)
    };

    companion object {
        val DEFAULT = DAYS
    }
}

enum class SessionStatisticsChartType : EnumWithLabel, EnumWithIcon {
    PIE {
        override val label = UiText.StringResource(R.string.statistics_session_statistics_chart_type_pie)
        override val icon = UiIcon.DynamicIcon(Icons.Default.PieChart)
    },
    BAR {
        override val label = UiText.StringResource(R.string.statistics_session_statistics_chart_type_bar)
        override val icon = UiIcon.DynamicIcon(Icons.Default.BarChart)
    };

    fun invert(): SessionStatisticsChartType {
        return when (this) {
            PIE -> BAR
            BAR -> PIE
        }
    }

    companion object {
        val DEFAULT = BAR
    }
}

data class TabWithTimeframe(
    val tab: SessionStatisticsTab,
    val timeframe: Timeframe
)

data class TabWithTimeframeWithSessions(
    val tabWithTimeframe: TabWithTimeframe,
    val sessions: List<SessionWithSectionsWithLibraryItems>
)

data class TabWithTimeframeWithSections(
    val tabWithTimeframe: TabWithTimeframe,
    val sections: List<Pair<ZonedDateTime, List<SectionWithLibraryItem>>>
)

data class SessionStatisticsUiState(
    val topBarUiState: SessionsStatisticsTopBarUiState,
    val contentUiState: SessionStatisticsContentUiState
)

data class SessionsStatisticsTopBarUiState(
    val chartType: SessionStatisticsChartType
)

data class SessionStatisticsContentUiState(
    val selectedTab: SessionStatisticsTab,
    val headerUiState: SessionStatisticsHeaderUiState,
    val barChartUiState: SessionStatisticsBarChartUiState?,
    val pieChartUiState: SessionStatisticsPieChartUiState?,
    val libraryItemsWithSelectionAndDuration: List<Triple<LibraryItem, Boolean, String>>
)

data class SessionStatisticsHeaderUiState(
    val seekBackwardEnabled: Boolean,
    val seekForwardEnabled: Boolean,
    val timeframe: Timeframe,
    val totalPracticeDuration: Duration,
)

data class SessionStatisticsBarChartUiState(
    val chartData: BarChartData,
)

data class BarChartData(
    val barData: List<BarChartDatum>,
    val maxDuration: Duration,
    val itemSortMode: LibraryItemSortMode,
    val itemSortDirection: SortDirection,
)

data class BarChartDatum(
    val label: String,
    val libraryItemsToDuration: Map<LibraryItem, Duration>,
    val totalDuration: Duration,
)

data class SessionStatisticsPieChartUiState(
    val chartData: PieChartData,
)

data class PieChartData(
    val libraryItemToDuration: Map<LibraryItem, Duration>,
    val itemSortMode: LibraryItemSortMode,
    val itemSortDirection: SortDirection
)

@HiltViewModel
class SessionStatisticsViewModel @Inject constructor(
    private val timeProvider: TimeProvider,
    libraryUseCases: LibraryUseCases,
    sessionsUseCases: SessionsUseCases
) : ViewModel() {

    /** Private variables */
    // convenient date because first of month is a monday
    private val release = timeProvider.getStartOfDay(dateTime = ZonedDateTime.parse("2022-08-01T00:00:00+02:00"))

    private var _barChartShowing = false
    private var _pieChartShowing = false

    private var _barChartStateBuffer: SessionStatisticsBarChartUiState? = null
    private var _pieChartStateBuffer: SessionStatisticsPieChartUiState? = null

    /** Imported Flows */
    private val itemsSortInfo = libraryUseCases.getItemSortInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortInfo(
            mode = LibraryItemSortMode.DEFAULT,
            direction = SortDirection.DEFAULT
        )
    )

    /** Own state flows */
    private val _chartType = MutableStateFlow(SessionStatisticsChartType.DEFAULT)
    private val _selectedTabWithTimeframe = MutableStateFlow(
        TabWithTimeframe(
            SessionStatisticsTab.DEFAULT,
            when (SessionStatisticsTab.DEFAULT) {
                SessionStatisticsTab.DAYS ->
                    timeProvider.getStartOfDay().minusDays(6) to timeProvider.getEndOfDay()
                SessionStatisticsTab.WEEKS ->
                    timeProvider.getStartOfWeek().minusWeeks(6) to timeProvider.getEndOfWeek()
                SessionStatisticsTab.MONTHS ->
                    timeProvider.getStartOfMonth().minusMonths(6) to timeProvider.getEndOfMonth()
            }
        ),
    )

    private val _deselectedLibraryItems = MutableStateFlow(emptySet<LibraryItem>())

    /** Combining imported and own state flows */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tabWithTimeframeWithSessions =
        _selectedTabWithTimeframe.flatMapLatest { tabWithTimeframe ->
            sessionsUseCases.getInTimeframe(tabWithTimeframe.timeframe).map {
                TabWithTimeframeWithSessions(
                    tabWithTimeframe = tabWithTimeframe,
                    sessions = it
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TabWithTimeframeWithSessions(
                tabWithTimeframe = _selectedTabWithTimeframe.value,
                sessions = emptyList()
            ),
        )

    private val tabWithTimeframeWithFilteredSections = combine(
        tabWithTimeframeWithSessions,
        _deselectedLibraryItems
    ) { (tabWithFrame, sessions), deselectedLibraryItems ->
        TabWithTimeframeWithSections(
            tabWithTimeframe = tabWithFrame,
            sections = sessions.map { session ->
                Pair(
                    session.startTimestamp,
                    session.sections.filter { (_, item) ->
                        item !in deselectedLibraryItems
                    }
                )
            }
        )
    }

    private val totalDuration = tabWithTimeframeWithFilteredSections.map { (_, sections) ->
        sections.sumOf { (_, sections) ->
            sections.sumOf { (section, _) -> section.duration.inWholeSeconds }
        }.seconds
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.seconds,
    )

    private val sortedLibraryItemsWithSelectionAndDuration = combine(
        tabWithTimeframeWithSessions,
        _deselectedLibraryItems,
        itemsSortInfo
    ) { (_, sessions), deselectedItems, (itemSortMode, itemSortDirection) ->
        val itemsToDurationTimeframe = sessions.flatMap { (_, sections) ->
            sections
        }.groupBy {
            it.libraryItem
        }.mapValues { (_, sections) ->
            sections.sumOf { (section, _) -> section.duration.inWholeSeconds }.seconds
        }

        itemsToDurationTimeframe.keys.toList().sorted(
            itemSortMode as LibraryItemSortMode,
            itemSortDirection
        ).map { item ->
            Triple(
                item,
                item !in deselectedItems,
                getDurationString(
                    duration = itemsToDurationTimeframe[item] ?: 0.seconds,
                    format = DurationFormat.HUMAN_PRETTY
                ).toString()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val barChartData = combine(
        _chartType,
        tabWithTimeframeWithFilteredSections,
        itemsSortInfo,
    ) {
            chartType,
            (tabWithTimeframe, timestampAndFilteredSections),
            (itemSortMode, itemSortDirection) ->

        if (chartType != SessionStatisticsChartType.BAR) return@combine null

        val (selectedTab, timeframe) = tabWithTimeframe
        val (_, timeframeEnd) = timeframe

        val timeframes = (0L..6L).reversed().map {
            when (selectedTab) {
                SessionStatisticsTab.DAYS ->
                    timeProvider.getStartOfDay(dateTime = timeframeEnd.minusSeconds(1)).minusDays(it) to
                        timeProvider.getEndOfDay(dateTime = timeframeEnd.minusSeconds(1)).minusDays(it)
                SessionStatisticsTab.WEEKS ->
                    timeProvider.getStartOfWeek(dateTime = timeframeEnd.minusSeconds(1)).minusWeeks(it) to
                        timeProvider.getEndOfWeek(dateTime = timeframeEnd.minusSeconds(1)).minusWeeks(it)
                SessionStatisticsTab.MONTHS ->
                    timeProvider.getStartOfMonth(dateTime = timeframeEnd.minusSeconds(1)).minusMonths(it) to
                        timeProvider.getEndOfMonth(dateTime = timeframeEnd.minusSeconds(1)).minusMonths(it)
            }
        }

        val specificDateToSections = timestampAndFilteredSections.groupBy { (timestamp, _) ->
            when (selectedTab) {
                SessionStatisticsTab.DAYS -> timestamp.specificDay
                SessionStatisticsTab.WEEKS -> timestamp.specificWeek
                SessionStatisticsTab.MONTHS -> timestamp.specificMonth
            }
        }.mapValues { (_, list) -> list.flatMap { (_, sections) -> sections } }

        val barData = timeframes.map { (start, end) ->
            val sectionsInBar = specificDateToSections[
                when (selectedTab) {
                    SessionStatisticsTab.DAYS -> start.specificDay
                    SessionStatisticsTab.WEEKS -> start.specificWeek
                    SessionStatisticsTab.MONTHS -> start.specificMonth
                }
            ] ?: emptyList()

            val libraryItemsToDurationInBar = sectionsInBar
                .groupBy { it.libraryItem }
                .mapValues { (_, sections) ->
                    sections.sumOf { (section, _) -> section.duration.inWholeSeconds }.seconds
                }

            BarChartDatum(
                label = when (selectedTab) {
                    SessionStatisticsTab.DAYS -> start.musikusFormat(DateFormat.WEEKDAY_ABBREVIATED)
                    SessionStatisticsTab.WEEKS -> "${start.dayOfMonth}-${end.minusSeconds(1).dayOfMonth}"
                    SessionStatisticsTab.MONTHS -> start.musikusFormat(DateFormat.MONTH_ABBREVIATED)
                },
                libraryItemsToDuration = libraryItemsToDurationInBar,
                totalDuration = libraryItemsToDurationInBar.values.sumOf { it.inWholeSeconds }.seconds
            )
        }

        BarChartData(
            barData = barData,
            itemSortMode = itemSortMode as LibraryItemSortMode,
            itemSortDirection = itemSortDirection,
            maxDuration = barData.maxOf { it.totalDuration }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    private val pieChartData = combine(
        _chartType,
        tabWithTimeframeWithFilteredSections,
        itemsSortInfo,
    ) { chartType, (_, timestampsInFrameWithFilteredSections), (itemSortMode, itemSortDirection) ->
        if (chartType != SessionStatisticsChartType.PIE) return@combine null

        val libraryItemToDuration = timestampsInFrameWithFilteredSections
            .unzip().second
            .flatten()
            .groupBy { it.libraryItem }
            .mapValues { (_, sections) ->
                sections.sumOf { (section, _) -> section.duration.inWholeSeconds }.seconds
            }

        PieChartData(
            libraryItemToDuration = libraryItemToDuration,
            itemSortMode = itemSortMode as LibraryItemSortMode,
            itemSortDirection = itemSortDirection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    /**
     *  Composing the Ui state
     */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pieChartUiState = pieChartData.flatMapLatest { chartData ->
        if (chartData == null) {
            return@flatMapLatest flow {
                if (_pieChartShowing) {
                    emit(
                        _pieChartStateBuffer?.let {
                            it.copy(
                                chartData = it.chartData.copy(
                                    libraryItemToDuration = emptyMap(),
                                )
                            )
                        }
                    )
                    _pieChartShowing = false
                    delay(700)
                }
                emit(null)
            }
        }

        flow {
            if (!_pieChartShowing) {
                emit(
                    SessionStatisticsPieChartUiState(
                        chartData = PieChartData(
                            libraryItemToDuration = emptyMap(),
                            itemSortMode = LibraryItemSortMode.DEFAULT,
                            itemSortDirection = SortDirection.DEFAULT
                        )
                    )
                )
                _pieChartShowing = true
                _pieChartStateBuffer = SessionStatisticsPieChartUiState(chartData)
                delay(700)
            }
            emit(SessionStatisticsPieChartUiState(chartData))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val barChartUiState = barChartData.flatMapLatest { chartData ->
        if (chartData == null) {
            return@flatMapLatest flow {
                if (_barChartShowing) {
                    emit(
                        _barChartStateBuffer?.let {
                            it.copy(
                                chartData = it.chartData.copy(
                                    barData = it.chartData.barData.map { bar ->
                                        bar.copy(
                                            libraryItemsToDuration = emptyMap(),
                                            totalDuration = 0.seconds,
                                        )
                                    },
                                )
                            )
                        }
                    )
                    _barChartShowing = false
                    delay(700)
                }
                emit(null)
            }
        }

        flow {
            if (!_barChartShowing) {
                _barChartStateBuffer = SessionStatisticsBarChartUiState(chartData)
                _barChartShowing = true
                delay(700)
            }
            emit(SessionStatisticsBarChartUiState(chartData))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    private val headerUiState = combine(
        _selectedTabWithTimeframe,
        totalDuration,
    ) { (_, timeframe), totalDuration ->
        val (start, end) = timeframe

        SessionStatisticsHeaderUiState(
            seekBackwardEnabled = start > release,
            seekForwardEnabled = end < timeProvider.getEndOfDay(),
            timeframe = timeframe,
            totalPracticeDuration = totalDuration,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionStatisticsHeaderUiState(
            timeframe = _selectedTabWithTimeframe.value.timeframe,
            totalPracticeDuration = totalDuration.value,
            seekBackwardEnabled = false,
            seekForwardEnabled = false,
        ),
    )

    private val contentUiState = combine(
        _selectedTabWithTimeframe,
        headerUiState,
        barChartUiState,
        pieChartUiState,
        sortedLibraryItemsWithSelectionAndDuration
    ) {
            (selectedTab, _),
            headerUiState,
            barChartUiState,
            pieChartUiState,
            libraryItemsWithSelectionAndDuration ->

        SessionStatisticsContentUiState(
            selectedTab = selectedTab,
            headerUiState = headerUiState,
            barChartUiState = barChartUiState,
            pieChartUiState = pieChartUiState,
            libraryItemsWithSelectionAndDuration = libraryItemsWithSelectionAndDuration,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionStatisticsContentUiState(
            selectedTab = _selectedTabWithTimeframe.value.tab,
            headerUiState = headerUiState.value,
            barChartUiState = barChartUiState.value,
            pieChartUiState = pieChartUiState.value,
            libraryItemsWithSelectionAndDuration = sortedLibraryItemsWithSelectionAndDuration.value,
        ),
    )

    private val topBarUiState = _chartType.map { chartType ->
        SessionsStatisticsTopBarUiState(
            chartType = chartType,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionsStatisticsTopBarUiState(
            chartType = _chartType.value,
        ),
    )

    val uiState = combine(
        topBarUiState,
        contentUiState
    ) { topBarUiState, contentUiState ->
        SessionStatisticsUiState(
            topBarUiState = topBarUiState,
            contentUiState = contentUiState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionStatisticsUiState(
            topBarUiState = topBarUiState.value,
            contentUiState = contentUiState.value,
        ),
    )

    /** Mutators */

    fun onChartTypeButtonClicked() {
        _chartType.update {
            when (_chartType.value) {
                SessionStatisticsChartType.BAR -> SessionStatisticsChartType.PIE
                SessionStatisticsChartType.PIE -> SessionStatisticsChartType.BAR
            }
        }
    }

    fun onTabSelected(selectedTab: SessionStatisticsTab) {
        if (selectedTab == _selectedTabWithTimeframe.value.tab) return
        _selectedTabWithTimeframe.update { (_, timeframe) ->
            val (_, end) = timeframe

            TabWithTimeframe(
                tab = selectedTab,
                timeframe = when (selectedTab) {
                    SessionStatisticsTab.DAYS -> {
                        val endOfToday = timeProvider.getEndOfDay()
                        var newEnd = end
                        if (newEnd > endOfToday) {
                            newEnd = endOfToday
                        }
                        timeProvider.getStartOfDay(dateTime = newEnd.minusSeconds(1)).minusDays(6) to newEnd
                    }
                    SessionStatisticsTab.WEEKS -> {
                        val endOfThisWeek = timeProvider.getEndOfWeek()
                        var newEnd = timeProvider.getEndOfWeek(dateTime = end.minusSeconds(1))
                        if (newEnd > endOfThisWeek) {
                            newEnd = endOfThisWeek
                        }
                        timeProvider.getStartOfWeek(dateTime = newEnd.minusSeconds(1)).minusWeeks(6) to newEnd
                    }
                    SessionStatisticsTab.MONTHS -> {
                        val endOfThisMonth = timeProvider.getEndOfMonth()
                        var newEnd = timeProvider.getEndOfMonth(dateTime = end.minusSeconds(1))
                        if (newEnd > endOfThisMonth) {
                            newEnd = endOfThisMonth
                        }
                        timeProvider.getStartOfMonth(dateTime = newEnd.minusSeconds(1)).minusMonths(6) to newEnd
                    }
                }
            )
        }
    }

    fun onSeekForwardClicked() {
        _selectedTabWithTimeframe.update { (tab, timeframe) ->
            val (_, end) = timeframe

            TabWithTimeframe(
                tab = tab,
                timeframe = when (tab) {
                    SessionStatisticsTab.DAYS -> {
                        val endOfToday = timeProvider.getEndOfDay()
                        var newEnd = end.plusDays(7)
                        if (newEnd > endOfToday) {
                            newEnd = endOfToday
                        }
                        timeProvider.getStartOfDay(dateTime = newEnd.minusSeconds(1)).minusDays(6) to newEnd
                    }
                    SessionStatisticsTab.WEEKS -> {
                        val endOfThisWeek = timeProvider.getEndOfWeek()
                        var newEnd = end.plusWeeks(7)
                        if (newEnd > endOfThisWeek) {
                            newEnd = endOfThisWeek
                        }
                        timeProvider.getStartOfWeek(dateTime = newEnd.minusSeconds(1)).minusWeeks(6) to newEnd
                    }
                    SessionStatisticsTab.MONTHS -> {
                        val endOfThisMonth = timeProvider.getEndOfMonth()
                        var newEnd = end.plusMonths(7)
                        if (newEnd > endOfThisMonth) {
                            newEnd = endOfThisMonth
                        }
                        timeProvider.getStartOfMonth(dateTime = newEnd.minusSeconds(1)).minusMonths(6) to newEnd
                    }
                }
            )
        }
    }

    fun onSeekBackwardClicked() {
        _selectedTabWithTimeframe.update { (tab, timeframe) ->
            val (start, _) = timeframe

            TabWithTimeframe(
                tab = tab,
                timeframe = when (tab) {
                    SessionStatisticsTab.DAYS -> {
                        var newStart = start.minusDays(7)
                        if (newStart < release) {
                            newStart = release
                        }
                        newStart to timeProvider.getEndOfDay(dateTime = newStart).plusDays(6)
                    }
                    SessionStatisticsTab.WEEKS -> {
                        var newStart = start.minusWeeks(7)
                        if (newStart < release) {
                            newStart = release
                        }
                        newStart to timeProvider.getEndOfWeek(dateTime = newStart).plusWeeks(6)
                    }
                    SessionStatisticsTab.MONTHS -> {
                        var newStart = start.minusMonths(7)
                        if (newStart < release) {
                            newStart = release
                        }
                        newStart to timeProvider.getEndOfMonth(dateTime = newStart).plusMonths(6)
                    }
                }
            )
        }
    }

    fun onLibraryItemCheckboxClicked(libraryItem: LibraryItem) {
        _deselectedLibraryItems.update {
            if (libraryItem in it) {
                it - libraryItem
            } else {
                it + libraryItem
            }
        }
    }
}
