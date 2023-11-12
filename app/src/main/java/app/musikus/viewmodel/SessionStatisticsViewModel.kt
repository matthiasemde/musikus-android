/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.dataStore
import app.musikus.database.MusikusDatabase
import app.musikus.database.daos.LibraryItem
import app.musikus.datastore.LibraryItemSortMode
import app.musikus.datastore.SortDirection
import app.musikus.datastore.sorted
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV
import app.musikus.utils.DATE_FORMATTER_PATTERN_WEEKDAY_SHORT
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
    val libraryItemsWithSelection: List<Pair<LibraryItem, Boolean>>
)

data class SessionStatisticsHeaderUiState(
    val timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
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
    val libraryItemsWithDuration: List<Pair<LibraryItem, Int>>,
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
    private val _showingLibraryItemsInPieChart = mutableSetOf<LibraryItem>()
    private val _showingLibraryItemsInBarChart = (1..7).map { mutableSetOf<LibraryItem>() }


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
    private val _selectedTab = MutableStateFlow(SessionStatisticsTab.DEFAULT)
    private val _timeFrame = MutableStateFlow(
        when(_selectedTab.value) {
            SessionStatisticsTab.DAYS -> getStartOfDay(dayOffset = -6) to getEndOfDay()
            SessionStatisticsTab.WEEKS -> getStartOfWeek(weekOffset = -6) to getEndOfWeek()
            SessionStatisticsTab.MONTHS -> getStartOfMonth(monthOffset = -6) to getEndOfMonth()
        },
    )

    private val _deselectedLibraryItems = MutableStateFlow(emptySet<LibraryItem>())

    /** Combining imported and own state flows */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sessionsInTimeFrame = _timeFrame.flatMapLatest { timeFrame ->
        sessionRepository.sessionsInTimeFrame(timeFrame)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val timestampsInFrameWithFilteredSections = combine(
        sessionsInTimeFrame,
        _deselectedLibraryItems,
    ) { sessions, deselectedLibraryItems ->
        sessions.map { (_, sections) ->
            Pair(
                sections.first().section.timestamp,
                sections.filter { (_, item) ->
                    item !in deselectedLibraryItems
                }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val totalDuration = timestampsInFrameWithFilteredSections.map { list ->
        list.sumOf { (_, sections) ->
            sections.sumOf { (section, _) -> section.duration }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0,
    )

    private val itemsInSessionsInTimeFrame = sessionsInTimeFrame.map { sessions ->
        sessions.flatMap { (_, sections) ->
            sections.map { it.libraryItem }
        }.distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val sortedLibraryItemsWithSelection = combine(
        itemsInSessionsInTimeFrame,
        _deselectedLibraryItems,
        itemsSortInfo
    ) { itemsInFrame, deselectedItems, (itemSortMode, itemSortDirection) ->
        itemsInFrame.sorted(
            itemSortMode,
            itemSortDirection
        ).map { it to (it !in deselectedItems) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val barChartData = combine(
        _chartType,
        _selectedTab,
        timestampsInFrameWithFilteredSections,
        itemsSortInfo,
    ) { chartType, selectedTab, timestampAndFilteredSections, (itemSortMode, itemSortDirection) ->
        if (chartType != SessionStatisticsChartType.BAR) return@combine null

        val timeFrames = (0L..6L).reversed().map {
            when(selectedTab) {
                SessionStatisticsTab.DAYS -> getStartOfDay(dayOffset = -it) to getEndOfDay(dayOffset = -it)
                SessionStatisticsTab.WEEKS -> getStartOfWeek(weekOffset = -it) to getEndOfWeek(weekOffset = -it)
                SessionStatisticsTab.MONTHS -> getStartOfMonth(monthOffset = -it) to getEndOfMonth(monthOffset = -it)
            }
        }

        val specificDateToSections = timestampAndFilteredSections.groupBy { (timestamp, _) ->
            when(selectedTab) {
                SessionStatisticsTab.DAYS -> getSpecificDay(timestamp)
                SessionStatisticsTab.WEEKS -> getSpecificWeek(timestamp)
                SessionStatisticsTab.MONTHS -> getSpecificMonth(timestamp)
            }
        }.mapValues { (_, list) -> list.flatMap {(_, sections) -> sections } }

        val barData = timeFrames.map { (start, end) ->
            val sectionsInBar = specificDateToSections[
                when(selectedTab) {
                    SessionStatisticsTab.DAYS -> getSpecificDay(getTimestamp(start))
                    SessionStatisticsTab.WEEKS -> getSpecificWeek(getTimestamp(start))
                    SessionStatisticsTab.MONTHS -> getSpecificMonth(getTimestamp(start))
                }
            ] ?: emptyList()

            val libraryItemsWithDurationInBar = sectionsInBar
                .groupBy { it.libraryItem }
                .mapValues { (_, sections) ->
                    sections.sumOf { (section, _) -> section.duration }
                }.toList()

            BarChartDatum(
                label = when(selectedTab) {
                    SessionStatisticsTab.DAYS -> start.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_WEEKDAY_SHORT))
                    SessionStatisticsTab.WEEKS -> "${start.dayOfMonth}-${end.dayOfMonth}"
                    SessionStatisticsTab.MONTHS -> start.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_MONTH_TEXT_ABBREV))
                },
                libraryItemsWithDuration = libraryItemsWithDurationInBar,
                totalDuration = libraryItemsWithDurationInBar.sumOf { (_, duration) -> duration }
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
        timestampsInFrameWithFilteredSections,
        itemsSortInfo,
    ) { chartType, timestampsInFrameWithFilteredSections, (itemSortMode, itemSortDirection) ->
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
    ).also { Log.d("pie-chart-vm", it.value.toString()) }

    /**
     *  Composing the Ui state
     */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pieChartUiState = pieChartData.flatMapLatest { chartData ->
        if (chartData == null) {
            _showingLibraryItemsInPieChart.clear()
            return@flatMapLatest flow { emit(null) }
        }

        flow {
            val filteredChartData = chartData.libraryItemToDuration.mapValues { (item, duration) ->
                if (item in _showingLibraryItemsInPieChart) duration else 0
            }.let { libraryItemsToFilteredDuration ->
                PieChartData(
                    libraryItemToDuration = libraryItemsToFilteredDuration,
                    itemSortMode = chartData.itemSortMode,
                    itemSortDirection = chartData.itemSortDirection
                )
            }

            emit(SessionStatisticsPieChartUiState(filteredChartData))

            val longDelay = _showingLibraryItemsInPieChart.isEmpty()
            _showingLibraryItemsInPieChart.addAll(
                chartData.libraryItemToDuration.map { (item, _) -> item }
            )
            delay(if (longDelay) 350 else 50) // small delay is necessary to allow first composition

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
            _showingLibraryItemsInBarChart.forEach { it.clear() }
            return@flatMapLatest flow { emit(null) }
        }

        flow {
            val filteredChartData = chartData.barData
                .zip(_showingLibraryItemsInBarChart)
                .map { (barData, itemsShowingInBar) ->
                barData.libraryItemsWithDuration.map { (item, duration) ->
                    item to if (item in itemsShowingInBar) duration else 0
                }.let { libraryItemsToFilteredDuration ->
                    barData.copy(
                        libraryItemsWithDuration = libraryItemsToFilteredDuration,
                        totalDuration = libraryItemsToFilteredDuration.sumOf { (_, duration) -> duration }
                    )
                }
            }.let { barChartDataWithFilteredDurations ->
                BarChartData(
                    barData = barChartDataWithFilteredDurations,
                    maxDuration = barChartDataWithFilteredDurations.maxOf { it.totalDuration },
                    itemSortMode = chartData.itemSortMode,
                    itemSortDirection = chartData.itemSortDirection,
                )
            }

            emit(SessionStatisticsBarChartUiState(filteredChartData))

            val longDelay = _showingLibraryItemsInBarChart.all { it.isEmpty() }
            _showingLibraryItemsInBarChart.zip(chartData.barData).forEach { (showingItems, barData) ->
                showingItems.addAll(
                    barData.libraryItemsWithDuration.map { (item, _) -> item }
                )
            }
            delay(if (longDelay) 350 else 50)

            emit(SessionStatisticsBarChartUiState(chartData))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    private val headerUiState = combine(
        _timeFrame,
        totalDuration,
    ) { timeFrame, totalDuration ->
        SessionStatisticsHeaderUiState(
            timeFrame = timeFrame,
            totalPracticeDuration = totalDuration,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionStatisticsHeaderUiState(
            timeFrame = _timeFrame.value,
            totalPracticeDuration = totalDuration.value,
        ),
    )

    private val contentUiState = combine(
        _selectedTab,
        headerUiState,
        barChartUiState,
        pieChartUiState,
        sortedLibraryItemsWithSelection
    ) { selectedTab, headerUiState, barChartUiState, pieChartUiState, libraryItemsWithSelection ->
        SessionStatisticsContentUiState(
            selectedTab = selectedTab,
            headerUiState = headerUiState,
            barChartUiState = barChartUiState,
            pieChartUiState = pieChartUiState,
            libraryItemsWithSelection = libraryItemsWithSelection,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionStatisticsContentUiState(
            selectedTab = _selectedTab.value,
            headerUiState = headerUiState.value,
            barChartUiState = barChartUiState.value,
            pieChartUiState = pieChartUiState.value,
            libraryItemsWithSelection = sortedLibraryItemsWithSelection.value,
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

    fun onTabSelected(tab: SessionStatisticsTab) {
        _selectedTab.update { tab }
        _timeFrame.update {
            when(tab) {
                SessionStatisticsTab.DAYS -> getStartOfDay(dayOffset = -6) to getEndOfDay()
                SessionStatisticsTab.WEEKS -> getStartOfWeek(weekOffset = -6) to getEndOfWeek()
                SessionStatisticsTab.MONTHS -> getStartOfMonth(monthOffset = -6) to getEndOfMonth()
            }
        }
    }

    fun onSeekForwardClicked() {
        _timeFrame.update {
            when(_selectedTab.value) {
                SessionStatisticsTab.DAYS -> it.first.plusDays(7) to it.second.plusDays(7)
                SessionStatisticsTab.WEEKS -> it.first.plusWeeks(7) to it.second.plusWeeks(7)
                SessionStatisticsTab.MONTHS -> it.first.plusMonths(7) to it.second.plusMonths(7)
            }
        }
    }

    fun onSeekBackwardClicked() {
        _timeFrame.update {
            when(_selectedTab.value) {
                SessionStatisticsTab.DAYS -> it.first.minusDays(7) to it.second.minusDays(7)
                SessionStatisticsTab.WEEKS -> it.first.minusWeeks(7) to it.second.minusWeeks(7)
                SessionStatisticsTab.MONTHS -> it.first.minusMonths(7) to it.second.minusMonths(7)
            }
        }
    }

    fun onLibraryItemCheckboxClicked(libraryItem: LibraryItem) {
        _deselectedLibraryItems.update {
            if (libraryItem in it) it - libraryItem
            else it + libraryItem
        }
    }
}