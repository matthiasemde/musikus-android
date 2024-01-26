/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.statistics.goalstatistics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.entities.GoalType
import app.musikus.ui.theme.libraryItemColors
import app.musikus.usecase.goals.GoalDescriptionWithInstancesWithProgressAndLibraryItems
import app.musikus.usecase.goals.GoalInstanceWithProgress
import app.musikus.usecase.goals.GoalsUseCases
import app.musikus.utils.DateFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.Timeframe
import app.musikus.utils.UiText
import app.musikus.utils.musikusFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    val target: Duration,
    val data: List<Pair<String, Duration>>,
    val uniqueColor: Color?,
    val redraw: Boolean
)

data class GoalStatisticsGoalSelectorUiState(
    val goalsInfo: List<GoalInfo>,
)

@HiltViewModel
class GoalStatisticsViewModel @Inject constructor(
    private val goalsUseCases: GoalsUseCases,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    /** Private variables */
    private var _redraw = true

    /** Private methods */
    private suspend fun seek(forward: Boolean) {
        _selectedGoal.update { selectedGoal ->
            if (selectedGoal == null) return@update null

            if (forward) {
                goalsUseCases.getNextNAfterInstance(
                    goalDescriptionWithLibraryItems = selectedGoal.goalDescriptionWithLibraryItems,
                    instanceId = selectedGoal.instances.last().id,
                    n = 7
                )
            } else {
                val previousInstanceId = selectedGoal.instances.first().previousInstanceId ?: return

                goalsUseCases.getLastNBeforeInstance(
                    goalDescriptionWithLibraryItems = selectedGoal.goalDescriptionWithLibraryItems,
                    instanceId = previousInstanceId,
                    n = 7
                )
            }
        }
    }

    /** Own state flows */
    private var _selectedGoal =
        MutableStateFlow<GoalDescriptionWithInstancesAndLibraryItems?>(null)


    /** Imported Flows */
    private val allGoals = goalsUseCases.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    /** Combining imported and own state flows */

    private val selectedGoalWithProgress = combine(
        _selectedGoal,
        allGoals,
    ) { selectedGoal, allGoals ->

        // if there are no goals, return null
        if (allGoals.isEmpty()) return@combine null

        // if there is no selected goal, select the first one
        if (selectedGoal == null) {
            val firstGoal = allGoals.first()
            _selectedGoal.update {
                goalsUseCases.getLastNBeforeInstance(
                    goalDescriptionWithLibraryItems = firstGoal.goalDescriptionWithLibraryItems,
                    instanceId = firstGoal.latestInstance.id,
                    n = 7
                )
            }
            return@combine null
        }

        val progress = goalsUseCases.calculateProgress(listOf(selectedGoal)).single()

        GoalDescriptionWithInstancesWithProgressAndLibraryItems(
            description = selectedGoal.description,
            instances = selectedGoal.instances
                .zip(progress)
                .map { (instance, progress) ->
                    GoalInstanceWithProgress(
                        instance = instance,
                        progress = progress
                    )
                },
            libraryItems = selectedGoal.libraryItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val goalToSuccessRate = allGoals.map { goals ->
        val goalProgressForDescriptions = goalsUseCases.calculateProgress(goals)

        goals.zip(goalProgressForDescriptions).associate { (goal, progressForInstances) ->
            val successRate = goal.instances.zip(progressForInstances).count { (instance, progress) ->
                progress >= instance.target
            } to goal.instances.size

            goal.description to successRate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    private val sortedGoalInfo = combine(
        allGoals,
        goalToSuccessRate,
        _selectedGoal
    ) { goals, goalToSuccessRate, selectedGoal ->
        if (goals.isEmpty()) return@combine null

        goals.map { goalDescriptionWithInstancesAndLibraryItems ->
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
                    ?.let { libraryItemColors[it.colorIndex] },
                successRate = goalToSuccessRate?.get(description),
                selected = description == selectedGoal?.description
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Composing the Ui state */

    private val barChartUiState = selectedGoalWithProgress.map { goalWithProgress ->
        if (goalWithProgress == null) return@map null

        val instancesWithProgress = goalWithProgress.instances

        // in case there are less than 7 instances, pad the list with nulls
        val paddedInstances =
            (0 until 7 - instancesWithProgress.size).map { null } +
            instancesWithProgress

        // map the instances to a list of pairs of date and progress
        val barData = paddedInstances.map { instanceWithProgress ->
            if(instanceWithProgress == null) "" to 0.seconds
            else Pair(
                instanceWithProgress.instance.startTimestamp.musikusFormat(DateFormat.DAY_AND_MONTH),
                instanceWithProgress.progress
            )
        }

        GoalStatisticsBarChartUiState(
            target = instancesWithProgress.maxBy { it.instance.startTimestamp }.instance.target,
            data = barData,
            uniqueColor = if(goalWithProgress.description.type == GoalType.ITEM_SPECIFIC) {
                libraryItemColors[goalWithProgress.libraryItems.first().colorIndex]
            } else null,
            redraw = _redraw.also { _redraw = false }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val headerUiState = selectedGoalWithProgress.map { goalWithProgress ->
        if (goalWithProgress == null) return@map null

        GoalStatisticsHeaderUiState(
            seekBackwardEnabled = goalWithProgress.instances.first().instance.previousInstanceId != null,
            seekForwardEnabled =
                goalWithProgress.instances.last().instance.endTimestamp != null &&
                goalWithProgress.description.archived.not()
            ,
            timeframe = goalWithProgress.instances.first().instance.startTimestamp to
                    goalWithProgress.instances.last().instance.startTimestamp,
            successRate = goalWithProgress.instances.count { (goal, progress) ->
                progress >= goal.target
            } to goalWithProgress.instances.size
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
        viewModelScope.launch {
            seek(forward = true)
        }
    }

    fun seekBackwards() {
        viewModelScope.launch {
            seek(forward = false)
        }
    }

    fun onGoalSelected(goalId: UUID) {
        viewModelScope.launch {
            if (_selectedGoal.value?.description?.id == goalId) return@launch

            val goal = allGoals.value.firstOrNull { it.description.id == goalId } ?: return@launch

            _redraw = true

            // if there is no selected goal, select the first one
            _selectedGoal.update {
                goalsUseCases.getLastNBeforeInstance(
                    goalDescriptionWithLibraryItems = goal.goalDescriptionWithLibraryItems,
                    instanceId = goal.latestInstance.id,
                    n = 7
                )
            }
        }
    }
}
