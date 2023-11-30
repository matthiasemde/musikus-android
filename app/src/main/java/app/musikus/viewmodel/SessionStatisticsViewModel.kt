/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.dataStore
import app.musikus.database.MusikusDatabase
import app.musikus.database.SectionWithLibraryItem
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.daos.LibraryItem
import app.musikus.datastore.LibraryItemSortMode
import app.musikus.datastore.SortDirection
import app.musikus.datastore.sorted
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV
import app.musikus.utils.DATE_FORMATTER_PATTERN_WEEKDAY_ABBREV
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.Timeframe
import app.musikus.utils.getDurationString
import app.musikus.utils.getEndOfDay
import app.musikus.utils.getEndOfMonth
import app.musikus.utils.getEndOfWeek
import app.musikus.utils.getSpecificDay
import app.musikus.utils.getSpecificMonth
import app.musikus.utils.getSpecificWeek
import app.musikus.utils.getStartOfDay
import app.musikus.utils.getStartOfMonth
import app.musikus.utils.getStartOfWeek
import app.musikus.utils.getTimestamp
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
import java.time.format.DateTimeFormatter

/**
 * Ui state data classes
 */

enum class SessionStatisticsTab {
    DAYS,
    WEEKS,
    MONTHS;

    companion object {
        val DEFAULT = DAYS
    }
}

enum class SessionStatisticsChartType {
    PIE,
    BAR;

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
    val totalPracticeDuration: Int,
)

data class SessionStatisticsBarChartUiState(
    val chartData: BarChartData,
)

data class BarChartData(
    val barData: List<BarChartDatum>,
    val maxDuration: Int,
    val itemSortMode: LibraryItemSortMode,
    val itemSortDirection: SortDirection,
)

data class BarChartDatum(
    val label: String,
    val libraryItemsToDuration: Map<LibraryItem, Int>,
    val totalDuration: Int,
)

data class SessionStatisticsPieChartUiState(
    val chartData: PieChartData,
)

data class PieChartData(
    val libraryItemToDuration: Map<LibraryItem, Int>,
    val itemSortMode: LibraryItemSortMode,
    val itemSortDirection: SortDirection
)

class SessionStatisticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Private variables */
    // convenient date because first of month is a monday
    private val release = getStartOfDay(dateTime = ZonedDateTime.parse("2022-08-01T00:00:00+02:00"))

    private var _barChartShowing = false
    private var _pieChartShowing = false

    private var _barChartStateBuffer: SessionStatisticsBarChartUiState? = null
    private var _pieChartStateBuffer: SessionStatisticsPieChartUiState? = null

    /** Database */
    private val database = MusikusDatabase.getInstance(application)

    /** Repositories */
    private val sessionRepository = SessionRepository(database)
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore)

    /** Imported Flows */
    private val itemsSortInfo = userPreferencesRepository.userPreferences.map {
        it.libraryItemSortMode to it.libraryItemSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryItemSortMode.DEFAULT to SortDirection.DEFAULT
    )

    /** Own state flows */
    private val _chartType = MutableStateFlow(SessionStatisticsChartType.DEFAULT)
//    private val _selectedTab = MutableStateFlow(SessionStatisticsTab.DEFAULT)
    private val _selectedTabWithTimeframe = MutableStateFlow(
        TabWithTimeframe(
            SessionStatisticsTab.DEFAULT,
            when(SessionStatisticsTab.DEFAULT) {
                SessionStatisticsTab.DAYS -> getStartOfDay(dayOffset = -6) to getEndOfDay()
                SessionStatisticsTab.WEEKS -> getStartOfWeek(weekOffset = -6) to getEndOfWeek()
                SessionStatisticsTab.MONTHS -> getStartOfMonth(monthOffset = -6) to getEndOfMonth()
            }
        ),
    )

    private val _deselectedLibraryItems = MutableStateFlow(emptySet<LibraryItem>())

    /** Combining imported and own state flows */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tabWithTimeframeWithSessions =
        _selectedTabWithTimeframe.flatMapLatest { tabWithTimeframe ->
        sessionRepository.sessionsInTimeframe(tabWithTimeframe.timeframe).map {
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

    private val tabWithTimeframeWithFilteredSections = combine (
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
            sections.sumOf { (section, _) -> section.duration }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0,
    )

    private val sortedLibraryItemsWithSelectionAndDuration = combine(
        tabWithTimeframeWithSessions,
        _deselectedLibraryItems,
        itemsSortInfo
    ) { (_, sessions), deselectedItems, (itemSortMode, itemSortDirection) ->
        val itemsToDurationTimeFrame = sessions.flatMap { (_, sections) ->
            sections
        }.groupBy {
            it.libraryItem
        }.mapValues { (_, sections) ->
            sections.sumOf { (section, _) -> section.duration }
        }

        itemsToDurationTimeFrame.keys.toList().sorted(
            itemSortMode,
            itemSortDirection
        ).map { item ->
            Triple (
                item,
                item !in deselectedItems,
                getDurationString(
                    durationSeconds = itemsToDurationTimeFrame[item] ?: 0,
                    format = TIME_FORMAT_HUMAN_PRETTY
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
        (tabWithTimeframe ,timestampAndFilteredSections),
        (itemSortMode, itemSortDirection) ->

        if (chartType != SessionStatisticsChartType.BAR) return@combine null

        val (selectedTab, timeframe) = tabWithTimeframe
        val (_, timeframeEnd) = timeframe

        val timeframes = (0L..6L).reversed().map {
            when(selectedTab) {
                SessionStatisticsTab.DAYS ->
                    getStartOfDay(dayOffset = -it, dateTime = timeframeEnd.minusSeconds(1)) to
                    getEndOfDay(dayOffset = -it, dateTime = timeframeEnd.minusSeconds(1))
                SessionStatisticsTab.WEEKS ->
                    getStartOfWeek(weekOffset = -it, dateTime = timeframeEnd.minusSeconds(1)) to
                    getEndOfWeek(weekOffset = -it, dateTime = timeframeEnd.minusSeconds(1))
                SessionStatisticsTab.MONTHS ->
                    getStartOfMonth(monthOffset = -it, dateTime = timeframeEnd.minusSeconds(1)) to
                    getEndOfMonth(monthOffset = -it, dateTime = timeframeEnd.minusSeconds(1))
            }
        }

        val specificDateToSections = timestampAndFilteredSections.groupBy { (timestamp, _) ->
            when(selectedTab) {
                SessionStatisticsTab.DAYS -> getSpecificDay(timestamp)
                SessionStatisticsTab.WEEKS -> getSpecificWeek(timestamp)
                SessionStatisticsTab.MONTHS -> getSpecificMonth(timestamp)
            }
        }.mapValues { (_, list) -> list.flatMap {(_, sections) -> sections } }

        val barData = timeframes.map { (start, end) ->
            val sectionsInBar = specificDateToSections[
                when(selectedTab) {
                    SessionStatisticsTab.DAYS -> getSpecificDay(getTimestamp(start))
                    SessionStatisticsTab.WEEKS -> getSpecificWeek(getTimestamp(start))
                    SessionStatisticsTab.MONTHS -> getSpecificMonth(getTimestamp(start))
                }
            ] ?: emptyList()

            val libraryItemsToDurationInBar = sectionsInBar
                .groupBy { it.libraryItem }
                .mapValues { (_, sections) ->
                    sections.sumOf { (section, _) -> section.duration }
                }

            BarChartDatum(
                label = when(selectedTab) {
                    SessionStatisticsTab.DAYS -> start.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_WEEKDAY_ABBREV))
                    SessionStatisticsTab.WEEKS -> "${start.dayOfMonth}-${end.minusSeconds(1).dayOfMonth}"
                    SessionStatisticsTab.MONTHS -> start.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV))
                },
                libraryItemsToDuration = libraryItemsToDurationInBar,
                totalDuration = libraryItemsToDurationInBar.values.sumOf { it }
            )
        }

        BarChartData(
            barData = barData,
            itemSortMode = itemSortMode,
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
                sections.sumOf { (section, _) -> section.duration }
            }

        PieChartData(
            libraryItemToDuration = libraryItemToDuration,
            itemSortMode = itemSortMode,
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
                if(_pieChartShowing) {
                    emit(_pieChartStateBuffer?.let { it.copy(
                        chartData = it.chartData.copy(
                            libraryItemToDuration = emptyMap(),
                        )
                    )})
                    _pieChartShowing = false
                    delay(700)
                }
                emit(null)
            }
        }

        flow {
            if (!_pieChartShowing) {
                emit(SessionStatisticsPieChartUiState(
                    chartData = PieChartData(
                        libraryItemToDuration = emptyMap(),
                        itemSortMode = LibraryItemSortMode.DEFAULT,
                        itemSortDirection = SortDirection.DEFAULT
                    )
                ))
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
                    emit(_barChartStateBuffer?.let { it.copy(
                        chartData = it.chartData.copy(
                            barData = it.chartData.barData.map { bar -> bar.copy(
                                libraryItemsToDuration = emptyMap(),
                                totalDuration = 0,
                            )},
                        )
                    )})
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
            seekForwardEnabled = end < getEndOfDay(),
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
            when(_chartType.value) {
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
                timeframe = when(selectedTab) {
                    SessionStatisticsTab.DAYS -> {
                        val endOfToday = getEndOfDay()
                        var newEnd = end
                        if (newEnd > endOfToday) {
                            newEnd = endOfToday
                        }
                        getStartOfDay(dayOffset = -6, dateTime = newEnd.minusSeconds(1)) to newEnd
                    }
                    SessionStatisticsTab.WEEKS -> {
                        val endOfThisWeek = getEndOfWeek()
                        var newEnd = getEndOfWeek(dateTime = end.minusSeconds(1))
                        if (newEnd > endOfThisWeek) {
                            newEnd = endOfThisWeek
                        }
                        getStartOfWeek(weekOffset = -6, dateTime = newEnd.minusSeconds(1)) to newEnd
                    }
                    SessionStatisticsTab.MONTHS -> {
                        val endOfThisMonth = getEndOfMonth()
                        var newEnd = getEndOfMonth(dateTime = end.minusSeconds(1))
                        if (newEnd > endOfThisMonth) {
                            newEnd = endOfThisMonth
                        }
                        getStartOfMonth(monthOffset = -6, dateTime = newEnd.minusSeconds(1)) to newEnd
                    }
                }
            )
        }
    }

    fun onSeekForwardClicked() {
        _selectedTabWithTimeframe.update { (tab , timeframe) ->
            val (_, end) = timeframe

            TabWithTimeframe(
                tab = tab,
                timeframe = when(tab) {
                    SessionStatisticsTab.DAYS -> {
                        val endOfToday = getEndOfDay()
                        var newEnd = end.plusDays(7)
                        if(newEnd > endOfToday) {
                           newEnd = endOfToday
                        }
                        getStartOfDay(dayOffset = -6, dateTime = newEnd.minusSeconds(1)) to newEnd
                    }
                    SessionStatisticsTab.WEEKS -> {
                        val endOfThisWeek = getEndOfWeek()
                        var newEnd = end.plusWeeks(7)
                        if(newEnd > endOfThisWeek) {
                           newEnd = endOfThisWeek
                        }
                        getStartOfWeek(weekOffset = -6, dateTime = newEnd.minusSeconds(1)) to newEnd
                    }
                    SessionStatisticsTab.MONTHS -> {
                        val endOfThisMonth = getEndOfMonth()
                        var newEnd = end.plusMonths(7)
                        if(newEnd > endOfThisMonth) {
                           newEnd = endOfThisMonth
                        }
                        getStartOfMonth(monthOffset = -6, dateTime = newEnd.minusSeconds(1)) to newEnd
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
                timeframe = when(tab) {
                    SessionStatisticsTab.DAYS -> {
                        var newStart = start.minusDays(7)
                        if(newStart < release) {
                            newStart = release
                        }
                        newStart to getEndOfDay(dayOffset = 6, dateTime = newStart)
                    }
                    SessionStatisticsTab.WEEKS -> {
                        var newStart = start.minusWeeks(7)
                        if(newStart < release) {
                            newStart = release
                        }
                        newStart to getEndOfWeek(weekOffset = 6, dateTime = newStart)
                    }
                    SessionStatisticsTab.MONTHS -> {
                        var newStart = start.minusMonths(7)
                        if(newStart < release) {
                            newStart = release
                        }
                        newStart to getEndOfMonth(monthOffset = 6, dateTime = newStart)
                    }
                }
            )
        }
    }

    fun onLibraryItemCheckboxClicked(libraryItem: LibraryItem) {
        _deselectedLibraryItems.update {
            if (libraryItem in it) it - libraryItem
            else it + libraryItem
        }
    }
}