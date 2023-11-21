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
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.datastore.GoalsSortMode
import app.musikus.datastore.SortDirection
import app.musikus.datastore.sorted
import app.musikus.repository.GoalRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.DATE_FORMATTER_PATTERN_DAY_AND_MONTH
import app.musikus.utils.TimeFrame
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
    val timeFrame: TimeFrame,
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

data class TimeFrameWithGoalsWithProgress(
    val timeFrame: TimeFrame,
    val goal: GoalInstanceWithDescriptionWithLibraryItems,
    val goalsWithProgress: List<GoalWithProgress>,
)

class GoalStatisticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Private variables */
    private var _redraw = true
    private val libraryColors = Musikus.getLibraryItemColors(application as Context)

    /** Private methods */

    private fun timeFrameForGoal(selectedGoal: GoalInstanceWithDescriptionWithLibraryItems)
    : TimeFrame {
        val end = getDateTimeFromTimestamp(
            timestamp = selectedGoal.instance.let { it.startTimestamp + it.periodInSeconds }
        )
        val startOffset = selectedGoal.description.description.periodInPeriodUnits.toLong() * 7

        return when(selectedGoal.description.description.periodUnit) {
            GoalPeriodUnit.DAY -> end.minusDays(startOffset) to end
            GoalPeriodUnit.WEEK -> end.minusWeeks(startOffset) to end
            GoalPeriodUnit.MONTH -> end.minusMonths(startOffset) to end
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
    }

    /** Own state flows */
    private val _timeFrameAndSelectedGoal =
        MutableStateFlow<Pair<TimeFrame, GoalInstanceWithDescriptionWithLibraryItems>?>(null)


    /** Combining imported and own state flows */



    @OptIn(ExperimentalCoroutinesApi::class)
    private val timeFrameWithGoalsWithProgress = combine(
        _timeFrameAndSelectedGoal,
        goals,
    ) { timeFrameAndSelectedGoal, goals ->

        // if there are no goals, return null
        if (goals.isEmpty()) return@combine null

        // if there is no selected goal, select the first one
        // if there is no time frame, initialize it according to the selected goal
        if (timeFrameAndSelectedGoal == null) {
            _timeFrameAndSelectedGoal.update {
                val selectedGoal = goals.first()
                timeFrameForGoal(selectedGoal) to selectedGoal
            }
            return@combine null
        }

        val (timeFrame, selectedGoal) = timeFrameAndSelectedGoal

        // if all the necessary data is available, return the filtered goals
        val (startTimestamp, endTimestamp) = timeFrame.toList().map { getTimestamp(it) }

        return@combine Triple(
            timeFrame,
            selectedGoal,
            goals.filter { goal ->
                (
                    goal.description.description.id == selectedGoal.description.description.id &&
                    goal.instance.startTimestamp in startTimestamp until endTimestamp
                )
            }
        )
    }.flatMapLatest { timeFrameWithGoals ->
        if (timeFrameWithGoals == null) return@flatMapLatest flowOf(null)

        val (timeFrame, selectedGoal, goals) = timeFrameWithGoals

        if (goals.isEmpty()) return@flatMapLatest flowOf(
            TimeFrameWithGoalsWithProgress(timeFrame, selectedGoal, emptyList())
        )

        // get a flow of sections for each goal
        combine(goals.map {goal ->
            sessionRepository.sectionsForGoal(goal).map {sections ->
                goal to sections
            }
        // and combine them into a single flow with a list of pairs of goals and progress
        }) { goalsWithSections ->
            TimeFrameWithGoalsWithProgress(
                timeFrame = timeFrame,
                goal = selectedGoal,
                goalsWithProgress = goalsWithSections.map { (goal, sections) ->
                    GoalWithProgress(
                        goal = goal,
                        progress = sections.sumOf { it.duration }
                    )
                }
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
        _timeFrameAndSelectedGoal
    ) { sortedGoals, goalToSuccessRate, timeFrameAndSelectedGoal ->
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
                selected = goal == timeFrameAndSelectedGoal?.let { (_, goal) -> goal }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Composing the Ui state */

    private val barChartUiState = timeFrameWithGoalsWithProgress.map { timeFrameWithGoalsWithProgress ->
        if (timeFrameWithGoalsWithProgress == null) return@map null

        val (_, end) = timeFrameWithGoalsWithProgress.timeFrame

        val selectedGoal = timeFrameWithGoalsWithProgress.goal
        val instance = selectedGoal.instance

        val goalDescriptionWithLibraryItems = selectedGoal.description
        val description = goalDescriptionWithLibraryItems.description

        val barTimeFrames = (0L..6L).reversed().map {
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

        val goalsWithProgress = timeFrameWithGoalsWithProgress.goalsWithProgress

        GoalStatisticsBarChartUiState(
            target = instance.target,
            data = barTimeFrames.map { (start, end) ->
                Pair(
                    start.format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN_DAY_AND_MONTH)),
                    goalsWithProgress.firstOrNull { goalWithProgress ->
                        goalWithProgress.goal.instance.startTimestamp in
                        getTimestamp(start) until getTimestamp(end)
                    }?.progress ?: 0
                )
            },
            uniqueColor = if(description.type == GoalType.ITEM_SPECIFIC) {
                Color(
                    libraryColors[goalDescriptionWithLibraryItems.libraryItems.first().colorIndex]
                )
            } else null,
            redraw = _redraw.also { _redraw = false }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val headerUiState = timeFrameWithGoalsWithProgress.map { timeFrameWithGoalsWithProgress ->
        if (timeFrameWithGoalsWithProgress == null) return@map null

        val goalsWithProgress = timeFrameWithGoalsWithProgress.goalsWithProgress

        GoalStatisticsHeaderUiState(
            seekBackwardEnabled = false, // TODO
            seekForwardEnabled = false, // TODO
            timeFrame = timeFrameWithGoalsWithProgress.timeFrame,
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
        if (_timeFrameAndSelectedGoal.value?.let {
                (_, selectedGoal) -> selectedGoal
        } == goal) return

        _redraw = true
        _timeFrameAndSelectedGoal.update {
            timeFrameForGoal(goal) to goal
        }
    }
}
