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
import app.musikus.repository.GoalRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import app.musikus.utils.DateFormat
import app.musikus.utils.GoalsSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.Timeframe
import app.musikus.utils.UiText
import app.musikus.utils.inLocalTimezone
import app.musikus.utils.musikusFormat
import app.musikus.utils.sorted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

data class GoalStatisticsUiState(
    val contentUiState: GoalStatisticsContentUiState
)

data class GoalStatisticsContentUiState(
    val headerUiState: GoalStatisticsHeaderUiState?,
    val barChartUiState: GoalStatisticsBarChartUiState?,
    val goalSelectorUiState: GoalStatisticsGoalSelectorUiState?,
)

data class GoalInfo(
    val goalId: UUID,
    val title: UiText,
    val subtitle: List<UiText>?,
    val uniqueColor: Color?,
    val successRate: Pair<Int, Int>?,
    val selected: Boolean,
) {
    override fun toString(): String {
        return "$goalId : $selected"
    }
}

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
            val (goal, timeframe) = pair ?: return@update null
            val (start, end) = timeframe ?: return@update null

            val goalStart = goal.startTime ?: return@update null
            val goalEnd = goal.endTime ?: return@update null

            var newStart = start
            var newEnd = end

            val periodInPeriodUnits = goal.description.periodInPeriodUnits.toLong()

            if (forward) {
                for (i in 0L..6L) {
                    val (tmpStart, tmpEnd) = when (goal.description.periodUnit) {
                        GoalPeriodUnit.DAY ->
                            newStart.plusDays(periodInPeriodUnits) to
                            newEnd.plusDays(periodInPeriodUnits)

                        GoalPeriodUnit.WEEK ->
                            newStart.plusWeeks(periodInPeriodUnits) to
                            newEnd.plusWeeks(periodInPeriodUnits)

                        GoalPeriodUnit.MONTH ->
                            newStart.plusMonths(periodInPeriodUnits) to
                            newEnd.plusMonths(periodInPeriodUnits)
                    }

                    if (tmpEnd > goalEnd) break

                    newStart = tmpStart
                    newEnd = tmpEnd
                }
            } else {
                if (start > goalStart) {
                    when (goal.description.periodUnit) {
                        GoalPeriodUnit.DAY -> {
                            newStart = newStart.minusDays(7 * periodInPeriodUnits)
                            newEnd = newEnd.minusDays(7 * periodInPeriodUnits)
                        }
                        GoalPeriodUnit.WEEK -> {
                            newStart = newStart.minusWeeks(7 * periodInPeriodUnits)
                            newEnd = newEnd.minusWeeks(7 * periodInPeriodUnits)
                        }

                        GoalPeriodUnit.MONTH -> {
                            newStart = newStart.minusMonths(7 * periodInPeriodUnits)
                            newEnd = newEnd.minusMonths(7 * periodInPeriodUnits)
                        }
                    }
                }
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
        sortedGoals,
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
        val (start, end) = timeframe ?: return@combine null

        Pair(
            timeframe,
            selectedGoal.copy(
                instances = selectedGoal.instances.filter { goalInstance ->
                    goalInstance.startTimestamp.inLocalTimezone().let {
                        start <= it && it < end
                    }
                }
            ),
        )
    }.flatMapLatest { timeframeWithFilteredGoal ->
        if (timeframeWithFilteredGoal == null) return@flatMapLatest flowOf(null)

        val (timeframe, goal) = timeframeWithFilteredGoal

        // get a flow of sections for each goal instance
        combine(goal.instances.map { instance ->
            sessionRepository.sectionsForGoal(
                instance = instance,
                description = goal.description,
                libraryItems = goal.libraryItems,
            ).map { sections ->
                instance to sections.sumOf { it.duration }
            }
        // and combine them into a single flow with a list of pairs of goals and progress
        }) { goalInstancesWithSections ->
            val goalWithInstancesWithProgress = GoalWithInstancesWithProgress(
                goal = GoalDescriptionWithLibraryItems(
                    description = goal.description,
                    libraryItems = goal.libraryItems
                ),
                instancesWithProgress = goalInstancesWithSections.toList()
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
        combine(
            goals.map { goalDescriptionWithInstancesAndLibraryItems ->
                combine(
                    goalDescriptionWithInstancesAndLibraryItems.instances.map { instance ->
                        sessionRepository.sectionsForGoal(
                            instance = instance,
                            description = goalDescriptionWithInstancesAndLibraryItems.description,
                            libraryItems = goalDescriptionWithInstancesAndLibraryItems.libraryItems
                        ).map { sections ->
                            sections.sumOf { it.duration } >= instance.target
                        }
                    }
                ) { successes ->
                    goalDescriptionWithInstancesAndLibraryItems.description to
                    (successes.count { it } to successes.size)
                }
            }
        ) {
            it.toMap()
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

            val title = description.title(
                item = goalDescriptionWithInstancesAndLibraryItems.libraryItems.firstOrNull()
            )

            val subtitle = goalDescriptionWithInstancesAndLibraryItems.subtitle

            GoalInfo(
                goalId = description.id,
                title = title,
                subtitle = subtitle,
                uniqueColor = goalDescriptionWithInstancesAndLibraryItems.libraryItems
                    .firstOrNull()
                    ?.let { Color(libraryColors[it.colorIndex]) },
                successRate = goalToSuccessRate?.get(description),
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
                    start.musikusFormat(DateFormat.DAY_AND_MONTH),
                    goalsWithProgress.firstOrNull { (goalInstance, _) ->
                        goalInstance.startTimestamp.inLocalTimezone().let {
                            start <= it && it < end
                        }
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
        )
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

    fun onGoalSelected(goalId: UUID) {
        if (_selectedGoalWithTimeframe.value?.let {
                (selectedGoal, _) -> selectedGoal.description.id
        } == goalId) return

        val goal = goals.value.firstOrNull { it.description.id == goalId } ?: return

        _redraw = true
        _selectedGoalWithTimeframe.update {
            goal to timeframeForGoal(goal)
        }
    }
}
