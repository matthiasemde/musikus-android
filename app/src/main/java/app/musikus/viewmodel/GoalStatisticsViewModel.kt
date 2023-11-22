/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.Musikus
import app.musikus.dataStore
import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.datastore.GoalsSortMode
import app.musikus.datastore.SortDirection
import app.musikus.datastore.sorted
import app.musikus.repository.GoalRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.DATE_FORMATTER_PATTERN_DAY_AND_MONTH
import app.musikus.utils.Timeframe
import app.musikus.utils.getTimestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.format.DateTimeFormatter

data class GoalStatisticsUiState(
    val contentUiState: GoalStatisticsContentUiState
)

data class GoalStatisticsContentUiState(
    val headerUiState: GoalStatisticsHeaderUiState?,
    val barChartUiState: GoalStatisticsBarChartUiState?,
    val goalSelectorUiState: GoalStatisticsGoalSelectorUiState?,
)

data class GoalInfo(
    val goal: GoalDescriptionWithInstancesAndLibraryItems,
    val successRate: Pair<Int, Int>?,
    val selected: Boolean,
)

data class GoalStatisticsHeaderUiState(
    val seekBackwardEnabled: Boolean,
    val seekForwardEnabled: Boolean,
    val timeframe: Timeframe,
    val successRate: Pair<Int, Int>?,
)
data class GoalStatisticsBarChartUiState(
    val target: Int,
    val data: List<Pair<String, Int>>,
    val uniqueColor: Color?,
    val redraw: Boolean
)

data class GoalStatisticsGoalSelectorUiState(
    val goalsInfo: List<GoalInfo>,
)

data class TimeframeWithGoalsWithProgress(
    val timeframe: Timeframe,
    val goalWithInstancesWithProgress: GoalWithInstancesWithProgress,
)

data class GoalWithInstancesWithProgress(
    val goal: GoalDescriptionWithLibraryItems,
    val instancesWithProgress: List<Pair<GoalInstance, Int>>,
)

class GoalStatisticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Private variables */
    private var _redraw = true
    private val libraryColors = Musikus.getLibraryItemColors(application as Context)

    /** Private methods */

    private fun timeframeForGoal(selectedGoal: GoalDescriptionWithInstancesAndLibraryItems)
    : Timeframe? {
        val end = selectedGoal.endTime ?: return null

        val offset = selectedGoal.description.periodInPeriodUnits.toLong() * 7

        return when(selectedGoal.description.periodUnit) {
            GoalPeriodUnit.DAY -> end.minusDays(offset) to end
            GoalPeriodUnit.WEEK -> end.minusWeeks(offset) to end
            GoalPeriodUnit.MONTH -> end.minusMonths(offset) to end
        }
    }

    private fun seek(forward: Boolean) {
        _selectedGoalWithTimeframe.update { pair ->
            Log.d("goal-stats-viewmodel", "seeking")
            val (goal, timeframe) = pair ?: return@update null
            val (start, end) = timeframe ?: return@update null

            val goalStart = goal.startTime ?: return@update null
            val goalEnd = goal.endTime ?: return@update null

            var newStart = start
            var newEnd = end

            val seekDirection = if(forward) 1L else -1L

            for (i in 0L..6L) {
                val (tmpStart, tmpEnd) = when (goal.description.periodUnit) {
                    GoalPeriodUnit.DAY ->
                        newStart.plusDays(seekDirection) to
                        newEnd.plusDays(seekDirection)

                    GoalPeriodUnit.WEEK ->
                        newStart.plusWeeks(seekDirection) to
                        newEnd.plusWeeks(seekDirection)

                    GoalPeriodUnit.MONTH ->
                        newStart.plusMonths(seekDirection) to
                        newEnd.plusMonths(seekDirection)
                }

                if(tmpStart < goalStart || tmpEnd > goalEnd) break

                newStart = tmpStart
                newEnd = tmpEnd
            }

            goal to (newStart to newEnd)
        }
    }


    /** Database */
    private val database = MusikusDatabase.getInstance(application)

    /** Repositories */
    private val sessionRepository = SessionRepository(database)
    private val goalRepository = GoalRepository(database)
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore)

    /** Imported Flows */
    private val goalSortInfo = userPreferencesRepository.userPreferences.map {
        it.goalsSortMode to it.goalsSortDirection
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsSortMode.DEFAULT to SortDirection.DEFAULT
    )

    private val goals = goalRepository.allGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val sortedGoals = combine(
        goals,
        goalSortInfo
    ) { goals, (sortMode, sortDirection) ->
        goals.sorted(sortMode, sortDirection)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Own state flows */
    private val _selectedGoalWithTimeframe =
        MutableStateFlow<Pair<GoalDescriptionWithInstancesAndLibraryItems, Timeframe?>?>(null)


    /** Combining imported and own state flows */



    @OptIn(ExperimentalCoroutinesApi::class)
    private val timeframeWithGoalsWithProgress = combine(
        _selectedGoalWithTimeframe,
        goals,
    ) { selectedGoalWithTimeframe, goals ->

        // if there are no goals, return null
        if (goals.isEmpty()) return@combine null

        // if there is no selected goal, select the first one
        // if there is no time frame, initialize it according to the selected goal
        if (selectedGoalWithTimeframe == null) {
            _selectedGoalWithTimeframe.update {
                val selectedGoal = goals.first()
                selectedGoal to timeframeForGoal(selectedGoal)
            }
            return@combine null
        }

        val (selectedGoal, timeframe) = selectedGoalWithTimeframe

        // if all the necessary data is available, return the filtered goals
        val (startTimestamp, endTimestamp) = timeframe?.toList()?.map {
            getTimestamp(it)
        } ?: return@combine null

        Pair(
            timeframe,
            selectedGoal.copy(
                instances = selectedGoal.instances.filter { instance ->
                    instance.startTimestamp in startTimestamp until endTimestamp
                }
            ),
        )
    }.flatMapLatest { timeframeWithFilteredGoal ->
        if (timeframeWithFilteredGoal == null) return@flatMapLatest flowOf(null)

        val (timeframe, goal) = timeframeWithFilteredGoal

        // get a flow of sections for each goal
        combine(goal.instances.map { instance ->
            sessionRepository.sectionsForGoal(
                instance = instance,
                libraryItems = goal.libraryItems
            ).map { sections ->
                instance to sections
            }
        // and combine them into a single flow with a list of pairs of goals and progress
        }) { goalsWithSections ->
            val goalWithInstancesWithProgress = GoalWithInstancesWithProgress(
                goal = GoalDescriptionWithLibraryItems(
                    description = goal.description,
                    libraryItems = goal.libraryItems
                ),
                instancesWithProgress = goalsWithSections.map { (instance, sections) ->
                    instance to sections.sumOf { it.duration }
                }
            )

            TimeframeWithGoalsWithProgress(
                timeframe = timeframe,
                goalWithInstancesWithProgress = goalWithInstancesWithProgress
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val goalToSuccessRate = goals.flatMapLatest { goals ->
        flow {
            delay(200)
            emit(goals.groupBy { it.description.id }.mapValues {
                5 to 13
            })
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    private val sortedGoalInfo = combine(
        sortedGoals,
        goalToSuccessRate,
        _selectedGoalWithTimeframe
    ) { sortedGoals, goalToSuccessRate, selectedGoalWithTimeframe ->
        if (sortedGoals.isEmpty()) return@combine null

        sortedGoals.map { goalDescriptionWithInstancesAndLibraryItems ->
            val description = goalDescriptionWithInstancesAndLibraryItems.description
            GoalInfo(
                goal = goalDescriptionWithInstancesAndLibraryItems,
                successRate = goalToSuccessRate?.get(description.id),
                selected = description == selectedGoalWithTimeframe?.let { (goal, _) -> goal.description }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Composing the Ui state */

    private val barChartUiState = timeframeWithGoalsWithProgress.map { timeframeWithGoalsWithProgress ->
        if (timeframeWithGoalsWithProgress == null) return@map null

        val (_, end) = timeframeWithGoalsWithProgress.timeframe

        val selectedGoal = timeframeWithGoalsWithProgress.goalWithInstancesWithProgress
        val lastInstance = selectedGoal.instancesWithProgress.lastOrNull()?.let { (instance, _) -> instance }
        val description = selectedGoal.goal.description

        val barTimeframes = (0L..6L).reversed().map {
            when(description.periodUnit) {
                GoalPeriodUnit.DAY -> {
                    end.minusDays((it + 1) * description.periodInPeriodUnits) to
                    end.minusDays(it * description.periodInPeriodUnits)
                }
                GoalPeriodUnit.WEEK -> {
                    end.minusWeeks((it + 1) * description.periodInPeriodUnits) to
                    end.minusWeeks(it * description.periodInPeriodUnits)
                }
                GoalPeriodUnit.MONTH -> {
                    end.minusMonths((it + 1) * description.periodInPeriodUnits) to
                    end.minusMonths(it * description.periodInPeriodUnits)
                }
            }
        }

        val goalsWithProgress = selectedGoal.instancesWithProgress

        GoalStatisticsBarChartUiState(
            target = lastInstance?.target ?: 0,
            data = barTimeframes.map { (start, end) ->
                Pair(
                    start.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_AND_MONTH)),
                    goalsWithProgress.firstOrNull { (goal, _) ->
                        goal.startTimestamp in
                        getTimestamp(start) until getTimestamp(end)
                    }?.let { (_, progress) -> progress } ?: 0
                )
            },
            uniqueColor = if(description.type == GoalType.ITEM_SPECIFIC) {
                Color(libraryColors[selectedGoal.goal.libraryItems.first().colorIndex])
            } else null,
            redraw = _redraw.also { _redraw = false }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val headerUiState = combine(
        _selectedGoalWithTimeframe,
        timeframeWithGoalsWithProgress,
    ) { selectedGoalWithTimeframe, timeframeWithGoalsWithProgress ->
        if (timeframeWithGoalsWithProgress == null) return@combine null

        val (selectedGoal, timeframe) = selectedGoalWithTimeframe ?: return@combine null
        val (start, end) = timeframe ?: return@combine null

        val goalWithInstancesWithProgress = timeframeWithGoalsWithProgress.goalWithInstancesWithProgress
        val goalsWithProgress = goalWithInstancesWithProgress.instancesWithProgress

        GoalStatisticsHeaderUiState(
            seekBackwardEnabled = start > selectedGoal.startTime,
            seekForwardEnabled = end < selectedGoal.endTime,
            timeframe = timeframeWithGoalsWithProgress.timeframe,
            successRate = goalsWithProgress.filter { (goal, progress) ->
                progress >= goal.target
            }.size to goalsWithProgress.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val contentUiState = combine(
        headerUiState,
        barChartUiState,
        sortedGoalInfo
    ) { headerUiState, barChartUiState, sortedGoalInfo ->
        GoalStatisticsContentUiState(
            headerUiState = headerUiState,
            barChartUiState = barChartUiState,
            goalSelectorUiState = sortedGoalInfo?.let {
                GoalStatisticsGoalSelectorUiState(it)
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalStatisticsContentUiState(
            headerUiState = null,
            barChartUiState = null,
            goalSelectorUiState = null
        )
    )

    val uiState = contentUiState.map { contentUiState ->
        GoalStatisticsUiState(
            contentUiState = contentUiState
        ).also { Log.d("goal-stats-viewmodel", "$it") }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalStatisticsUiState(
            contentUiState = contentUiState.value
        )
    )

    /** State mutators */

    fun seekForwards() {
        seek(forward = true)
    }

    fun seekBackwards() {
        seek(forward = false)
    }

    fun onGoalSelected(goal: GoalDescriptionWithInstancesAndLibraryItems) {
        if (_selectedGoalWithTimeframe.value?.let {
                (selectedGoal, _) -> selectedGoal
        } == goal) return

        _redraw = true
        _selectedGoalWithTimeframe.update {
            goal to timeframeForGoal(goal)
        }
    }
}
