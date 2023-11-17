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
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.datastore.GoalsSortMode
import app.musikus.datastore.SortDirection
import app.musikus.datastore.sorted
import app.musikus.repository.GoalRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.DATE_FORMATTER_PATTERN_DAY_AND_MONTH
import app.musikus.utils.getDateTimeFromTimestamp
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
import java.time.ZonedDateTime
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
    val goal: GoalInstanceWithDescriptionWithLibraryItems,
    val successRate: Pair<Int, Int>?,
    val selected: Boolean,
)

data class GoalStatisticsHeaderUiState(
    val seekBackwardEnabled: Boolean,
    val seekForwardEnabled: Boolean,
    val timeFrame: Pair<ZonedDateTime, ZonedDateTime>,
    val successRate: Pair<Int, Int>?,
)
data class GoalStatisticsBarChartUiState(
    val target: Int,
    val data: List<Pair<String, Int>>,
    val redraw: Boolean
)

data class GoalStatisticsGoalSelectorUiState(
    val goalsInfo: List<GoalInfo>,
)

class GoalStatisticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Private variables */
    private val _selectedGoal =
        MutableStateFlow<GoalInstanceWithDescriptionWithLibraryItems?>(null)

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
    }

    /** Own state flows */
    private val _timeFrame = MutableStateFlow<Pair<ZonedDateTime, ZonedDateTime>?>(null)
    private val _redraw = MutableStateFlow(true)

    /** Combining imported and own state flows */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedGoalsInTimeFrameWithProgress = combine(
        _timeFrame,
        goals,
        _selectedGoal,
    ) { timeFrame, goals, selectedGoal ->
        // if there are no goals, return null
        if (goals.isEmpty()) return@combine null

        // if there is no selected goal, select the first one
        if (selectedGoal == null) {
            _selectedGoal.update { goals.first() }
            return@combine null
        }

        // if there is no time frame, initialize it according to the selected goal
        if (timeFrame == null) {
            _timeFrame.update { _ ->
                val end = getDateTimeFromTimestamp(
                    timestamp = selectedGoal.instance.let { it.startTimestamp + it.periodInSeconds }
                )
                val startOffset = selectedGoal.description.description.periodInPeriodUnits.toLong() * 7
                when(selectedGoal.description.description.periodUnit) {
                    GoalPeriodUnit.DAY -> end.minusDays(startOffset) to end
                    GoalPeriodUnit.WEEK -> end.minusWeeks(startOffset) to end
                    GoalPeriodUnit.MONTH -> end.minusMonths(startOffset) to end
                }
            }
            return@combine null
        }

        // if all the necessary data is available, return the filtered goals
        val (startTimestamp, endTimestamp) = timeFrame.toList().map { getTimestamp(it) }

        goals.filter { goal ->
            (
                goal.description.description.id == selectedGoal.description.description.id &&
                goal.instance.startTimestamp in startTimestamp until endTimestamp
            )
        }.also {
            Log.d("goal-stats-viewmodel", "selectedGoalsInTime: $it")
        }
    }.flatMapLatest { goalsInTimeFrame ->
        if (goalsInTimeFrame == null) return@flatMapLatest flowOf(null)

        if (goalsInTimeFrame.isEmpty()) return@flatMapLatest flowOf(emptyList())

        // get a flow of sections for each goal
        combine(goalsInTimeFrame.map {goal ->
            sessionRepository.sectionsForGoal(goal).map {sections ->
                goal to sections
            }
        // and combine them into a single flow with a list of pairs of goals and progress
        }) { goalsWithSections ->
            goalsWithSections.map { (goal, sections) ->
                goal to sections.sumOf { it.duration }
            }
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
            emit(goals.groupBy { it.description.description.id }.mapValues {
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
        _selectedGoal
    ) { sortedGoals, goalToSuccessRate, selectedGoal ->
        if (sortedGoals.isEmpty()) return@combine null
        sortedGoals.groupBy { (_, descriptionWithLibraryItems) ->
            descriptionWithLibraryItems
        }.mapValues { (_, instances) ->
            instances.maxBy { (instance, _) ->
                instance.startTimestamp
            }
        }.map { (_, goal) ->
            val descriptionWithLibraryItems = goal.description
            val description = descriptionWithLibraryItems.description
            GoalInfo(
                goal = goal,
                successRate = goalToSuccessRate?.get(description.id),
                selected = goal == selectedGoal
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Composing the Ui state */

    private val barChartUiState = combine(
        _timeFrame,
        _selectedGoal,
        selectedGoalsInTimeFrameWithProgress,
        _redraw
    ) { timeFrame, selectedGoal, goalsWithProgress, redraw ->
        if (
            timeFrame == null ||
            selectedGoal == null ||
            goalsWithProgress == null
        ) return@combine null

        val (_, end) = timeFrame

        val instance = selectedGoal.instance

        val goalDescriptionWithLibraryItems = selectedGoal.description
        val description = goalDescriptionWithLibraryItems.description

        val timeFrames = (0L..6L).reversed().map {
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
        GoalStatisticsBarChartUiState(
            target = instance.target,
            data = timeFrames.map { (start, end) ->
                Pair(
                    start.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_AND_MONTH)),
                    goalsWithProgress.firstOrNull { (goal, _) ->
                        goal.instance.startTimestamp in getTimestamp(start) until getTimestamp(end)
                    }?.let { (_, progress) -> progress } ?: 0
                )
            },
            redraw = redraw.also { _redraw.update { false } }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val headerUiState = combine(
        _timeFrame,
        selectedGoalsInTimeFrameWithProgress
    ) { timeFrame, goalsWithProgress ->
        if (timeFrame == null || goalsWithProgress == null) return@combine null

        GoalStatisticsHeaderUiState(
            seekBackwardEnabled = false, // TODO
            seekForwardEnabled = false, // TODO
            timeFrame = timeFrame,
            successRate = goalsWithProgress.filter { (goal, progress) ->
                progress >= goal.instance.target
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
        TODO()
    }

    fun seekBackwards() {
        TODO()
    }

    fun onGoalSelected(goal: GoalInstanceWithDescriptionWithLibraryItems) {
        if (_selectedGoal.value == goal) return
        _selectedGoal.update { goal }
        _timeFrame.update { null } // reset timeFrame
    }


}
