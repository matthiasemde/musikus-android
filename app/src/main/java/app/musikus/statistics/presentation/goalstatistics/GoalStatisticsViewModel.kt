/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.statistics.presentation.goalstatistics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.core.data.GoalDescriptionWithLibraryItems
import app.musikus.goals.data.entities.GoalType
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.goals.domain.usecase.GoalsUseCases
import app.musikus.utils.DateFormat
import app.musikus.utils.Timeframe
import app.musikus.utils.UiText
import app.musikus.utils.musikusFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

data class GoalSelection(
    val description: GoalDescriptionWithLibraryItems,
    val instanceId: UUID,
    val isLast: Boolean,
)

@HiltViewModel
class GoalStatisticsViewModel @Inject constructor(
    private val goalsUseCases: GoalsUseCases,
) : ViewModel() {

    /** Private variables */
    private var _redraw = true

    /** Private methods */
    private fun seek(forward: Boolean) {
        _goalSelection.update {
            val goalSelection = it ?: return@update null
            val selectedGoalWithProgress = selectedGoalWithProgress.value ?: return@update null
            if (forward) {
                goalSelection.copy(
                    instanceId = selectedGoalWithProgress
                        .instancesWithProgress.last().let { (instance, _) ->
                            instance.id
                        },
                    isLast = false
                )
            } else {
                val previousLastInstanceId = selectedGoalWithProgress
                    .instancesWithProgress.first().let { (instance, _) ->
                        instance.previousInstanceId
                    } ?: return@update null
                goalSelection.copy(
                    instanceId = previousLastInstanceId,
                    isLast = true
                )
            }
        }
    }

    /** Own state flows */
    private val _goalSelection = MutableStateFlow<GoalSelection?>(null)

    /** Imported Flows */
    private val allGoals = goalsUseCases.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    /** Combining imported and own state flows */

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedGoalWithProgress = combine(
        allGoals,
        _goalSelection,
    ) { allGoals, goalSelection ->

        // if there are no goals, return null
        if (allGoals.isEmpty()) return@combine null

        // if there is no selected goal, select the first one
        if (goalSelection == null) {
            _goalSelection.update {
                val firstGoal = allGoals.first()
                GoalSelection(
                    description = firstGoal.goalDescriptionWithLibraryItems,
                    instanceId = firstGoal.latestInstance.id,
                    isLast = true
                )
            }
            return@combine null
        }

        if (goalSelection.isLast) {
            goalsUseCases.getLastNBeforeInstance(
                goalDescriptionWithLibraryItems = goalSelection.description,
                lastInstanceId = goalSelection.instanceId,
                n = 7
            )
        } else {
            goalsUseCases.getNextNAfterInstance(
                goalDescriptionWithLibraryItems = goalSelection.description,
                firstInstanceId = goalSelection.instanceId,
                n = 7
            )
        }

    }.flatMapLatest { it ?: flowOf(null) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val goalToSuccessRate = allGoals.flatMapLatest { goals ->
        goalsUseCases.calculateProgress(goals).map { goalProgressForDescriptions ->

            goals.zip(goalProgressForDescriptions).associate { (goal, progressForInstances) ->
                val successRate = goal.instances.zip(progressForInstances).count { (instance, progress) ->
                    progress >= instance.target
                } to goal.instances.size

                goal.description to successRate
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    private val sortedGoalInfo = combine(
        allGoals,
        goalToSuccessRate,
        _goalSelection
    ) { goals, goalToSuccessRate, goalSelection ->
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
                selected = description == goalSelection?.description?.description
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

        val instancesWithProgress = goalWithProgress.instancesWithProgress

        // in case there are less than 7 instances, pad the list with nulls
        val paddedInstances =
            (0 until 7 - instancesWithProgress.size).map { null } +
            instancesWithProgress

        // map the instances to a list of pairs of date and progress
        val barData = paddedInstances.map barData@ { instanceWithProgress ->
            val (instance, progress) = instanceWithProgress ?: return@barData "" to 0.seconds
            Pair(
                instance.startTimestamp.musikusFormat(DateFormat.DAY_AND_MONTH),
                progress
            )
        }

        GoalStatisticsBarChartUiState(
            target = instancesWithProgress
                .maxBy { (instance, _) -> instance.startTimestamp }
                .let { (instance, _) -> instance.target },
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

        val instances = goalWithProgress.instancesWithProgress.unzip().first

        GoalStatisticsHeaderUiState(
            seekBackwardEnabled = instances.first().previousInstanceId != null,
            seekForwardEnabled =
                instances.last().endTimestamp != null &&
                goalWithProgress.description.archived.not(),
            timeframe = instances.first().startTimestamp to
                instances.last().startTimestamp,
            successRate = goalWithProgress.instancesWithProgress.count { (goal, progress) ->
                progress >= goal.target
            } to goalWithProgress.instancesWithProgress.size
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
            if (_goalSelection.value?.description?.description?.id == goalId) return@launch

            val goal = allGoals.value.firstOrNull { it.description.id == goalId } ?: return@launch

            _redraw = true

            _goalSelection.update {
                GoalSelection(
                    description = goal.goalDescriptionWithLibraryItems,
                    instanceId = goal.latestInstance.id,
                    isLast = true
                )
            }
        }
    }
}