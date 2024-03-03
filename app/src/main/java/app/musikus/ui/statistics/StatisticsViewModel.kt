/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.statistics

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.ui.theme.libraryItemColors
import app.musikus.usecase.goals.GoalsUseCases
import app.musikus.usecase.sessions.SessionsUseCases
import app.musikus.utils.DateFormat
import app.musikus.utils.TimeProvider
import app.musikus.utils.getDayIndexOfWeek
import app.musikus.utils.musikusFormat
import app.musikus.utils.specificMonth
import app.musikus.utils.weekIndexToName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * Ui state data classes
 */
data class StatisticsUiState(
    val contentUiState: StatisticsContentUiState,
)
data class StatisticsContentUiState(
    val currentMonthUiState: StatisticsCurrentMonthUiState?,
    val practiceDurationCardUiState: StatisticsPracticeDurationCardUiState?,
    val goalCardUiState: StatisticsGoalCardUiState?,
    val ratingsCardUiState: StatisticsRatingsCardUiState?,
    val showHint: Boolean,
)

data class StatisticsCurrentMonthUiState(
    val totalPracticeDuration: Duration,
    val averageDurationPerSession: Duration,
    val breakDurationPerHour: Duration,
    val averageRatingPerSession: Float,
)

data class StatisticsPracticeDurationCardUiState(
    val lastSevenDayPracticeDuration: List<PracticeDurationPerDay>,
    val totalPracticeDuration: Duration,
)

data class PracticeDurationPerDay(
    val day: String,
    val duration: Duration,
)

data class GoalCardGoalDisplayData(
    val label: String,
    val progress: Float,
    val color: Color?
)

data class StatisticsGoalCardUiState(
    val successRate: Pair<Int, Int>?,
    val lastGoalsDisplayData: List<GoalCardGoalDisplayData>,
)

data class StatisticsRatingsCardUiState(
    val numOfRatingsFromOneToFive: List<Int>,
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    timeProvider: TimeProvider,
    goalsUseCases: GoalsUseCases,
    sessionsUseCases: SessionsUseCases,
) : ViewModel() {

    /** Imported flows */

    private val _timeframe = Pair(
        minOf(
            timeProvider.getStartOfMonth(),
            timeProvider.getStartOfDay(dayOffset = -7)
        ),
        timeProvider.getEndOfMonth(),
    )

    private val sessions = sessionsUseCases.getInTimeframe(_timeframe).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val lastFiveCompletedGoals = goalsUseCases.getLastFiveCompleted().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    /**
     *  Composing the Ui state
     */

    private val currentMonthUiState = sessions.map { sessions ->
        if (sessions.isEmpty()) return@map null

        val currentSpecificMonth = timeProvider.now().specificMonth
        val currentMonthSessions = sessions.filter { session ->
            session.startTimestamp.specificMonth == currentSpecificMonth
        }
        val totalPracticeDuration = currentMonthSessions.sumOf { (_, sections) ->
            sections.sumOf { (section, _) -> section.duration.inWholeSeconds }
        }.seconds
        val averageDurationPerSession = currentMonthSessions.size.let {
            if(it == 0) 0.seconds else totalPracticeDuration / it
        }
        val breakDurationPerHour = currentMonthSessions.sumOf { (session, _) ->
            session.breakDuration.inWholeSeconds
        }.seconds
        val averageRatingPerSession = currentMonthSessions.size.let {
            if(it == 0) 0f else
            currentMonthSessions.sumOf { (session, _) ->
                session.rating
            }.toFloat() / it
        }

        StatisticsCurrentMonthUiState(
            totalPracticeDuration = totalPracticeDuration,
            averageDurationPerSession = averageDurationPerSession,
            breakDurationPerHour = breakDurationPerHour,
            averageRatingPerSession = averageRatingPerSession,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private var _noSessionsForDurationCard = true

    @OptIn(ExperimentalCoroutinesApi::class)
    private val practiceDurationCardUiState = sessions.flatMapLatest { sessions ->
        if (sessions.isEmpty()) {
            _noSessionsForRatingCard = true
            return@flatMapLatest flow { emit(null) }
        }

        val lastSevenDays = (0..6).reversed().map { dayOffset ->
            (getDayIndexOfWeek(dateTime = timeProvider.now()) - dayOffset).let {
                timeProvider.getStartOfDayOfWeek(
                    dayIndex = (it-1).mod(7).toLong() + 1,
                    weekOffset = if (it > 0) 0 else -1
                )
            }
        }

        val groupedSessions = sessions.filter { session ->
            session.startTimestamp > lastSevenDays.first()
        }.groupBy { session ->
            getDayIndexOfWeek(session.startTimestamp)
        }

        val lastSevenDayPracticeDuration = lastSevenDays.map { day ->
            val dayIndex = getDayIndexOfWeek(day)
            PracticeDurationPerDay(
                day = weekIndexToName(dayIndex)[0].toString(),
                duration = (groupedSessions[dayIndex]?.sumOf { (_, sections) ->
                    sections.sumOf { (section, _) -> section.duration.inWholeSeconds }
                } ?: 0).seconds
            )
        }

        val totalPracticeDuration = lastSevenDayPracticeDuration.sumOf {
            it.duration.inWholeSeconds
        }.seconds

        flow {
            if (_noSessionsForDurationCard) {
                emit(
                    StatisticsPracticeDurationCardUiState(
                    lastSevenDayPracticeDuration = lastSevenDayPracticeDuration.map { PracticeDurationPerDay(
                        day = it.day,
                        duration = 0.seconds
                    ) },
                    totalPracticeDuration = totalPracticeDuration,
                )
                )
                delay(350)
                _noSessionsForDurationCard = false
            }
            emit(
                StatisticsPracticeDurationCardUiState(
                    lastSevenDayPracticeDuration = lastSevenDayPracticeDuration,
                    totalPracticeDuration = totalPracticeDuration,
                )
            )
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private var _noSessionsForGoalCard = true

    @OptIn(ExperimentalCoroutinesApi::class)
    private val goalCardUiState = lastFiveCompletedGoals.flatMapLatest { lastFiveGoals ->
        if (lastFiveGoals.isEmpty()) {
            _noSessionsForGoalCard = true
            return@flatMapLatest flow { emit(null) }
        }

        flow {
            if (_noSessionsForGoalCard) {
                emit(StatisticsGoalCardUiState(
                    successRate = null,
                    lastGoalsDisplayData = lastFiveGoals.map {
                        GoalCardGoalDisplayData(
                            label = "",
                            progress = 0f,
                            color = null
                        )
                    }
                ))
                delay(350)
                _noSessionsForGoalCard = false
            }
            emit(StatisticsGoalCardUiState(
                successRate = lastFiveGoals.count {
                    it.progress >= it.instance.target
                } to lastFiveGoals.size,
                lastGoalsDisplayData = lastFiveGoals.map {
                    GoalCardGoalDisplayData(
                        label = it.instance.startTimestamp.musikusFormat(DateFormat.DAY_AND_MONTH),
                        progress = (
                            it.progress.inWholeSeconds.toFloat() / it.instance.target.inWholeSeconds
                        ).coerceAtMost(1f),
                        color = it.description.libraryItems.firstOrNull()?.let { item ->
                            libraryItemColors[item.colorIndex]
                        }
                    )
                }
            ))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private var _noSessionsForRatingCard = true

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ratingsCardUiState = sessions.flatMapLatest { sessions ->
        if (sessions.isEmpty()) {
            _noSessionsForRatingCard = true
            return@flatMapLatest flow { emit(null) }
        }

        val numOfRatingsFromOneToFive = sessions.groupBy { (session, _) ->
            session.rating
        }.let { ratingToSessions ->
            (1 .. 5).map { rating ->
                ratingToSessions[rating]?.size ?: 0
            }
        }

        flow {
            if (_noSessionsForRatingCard) {
                emit(StatisticsRatingsCardUiState((1..5).map { 0 }))
                delay(350)
                _noSessionsForRatingCard = false
            }
            emit(StatisticsRatingsCardUiState(numOfRatingsFromOneToFive))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(50000),
        initialValue = null
    )

    val uiState = combine(
        currentMonthUiState,
        practiceDurationCardUiState,
        goalCardUiState,
        ratingsCardUiState,
    ) { currentMonthUiState, practiceDurationCardUiState, goalCardUiState, ratingsCardUiState ->
        StatisticsUiState(
            contentUiState = StatisticsContentUiState(
                currentMonthUiState = currentMonthUiState,
                practiceDurationCardUiState = practiceDurationCardUiState,
                goalCardUiState = goalCardUiState,
                ratingsCardUiState = ratingsCardUiState,
                showHint = (
                    currentMonthUiState == null &&
                    practiceDurationCardUiState == null &&
                    goalCardUiState == null &&
                    ratingsCardUiState == null
                )
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(50000),
        initialValue = StatisticsUiState(
            contentUiState = StatisticsContentUiState(
                currentMonthUiState = currentMonthUiState.value,
                practiceDurationCardUiState = practiceDurationCardUiState.value,
                goalCardUiState = goalCardUiState.value,
                ratingsCardUiState = ratingsCardUiState.value,
                showHint = true
            ),
        )
    )

    override fun onCleared() {
        Log.d("StatisticsViewModel", "onCleared: ")
        super.onCleared()
    }
}