/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.statistics.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.getDayIndexOfWeek
import app.musikus.core.domain.musikusFormat
import app.musikus.core.domain.specificMonth
import app.musikus.core.domain.weekIndexToName
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.goals.domain.usecase.GoalsUseCases
import app.musikus.sessions.domain.usecase.SessionsUseCases
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

data class PracticeDurationPerDay(
    val day: String = "",
    val duration: Duration = 0.seconds,
)

data class GoalCardGoalDisplayData(
    val label: String = "",
    val progress: Float = 0.0f,
    val color: Color? = null
)

/**
 * Ui state data classes
 */
data class StatisticsUiState(
    val contentUiState: StatisticsContentUiState,
)
data class StatisticsContentUiState(
    val currentMonthUiState: StatisticsCurrentMonthUiState?,
    val practiceDurationCardUiState: StatisticsSessionsCardUiState?,
    val goalCardUiState: StatisticsGoalsCardUiState?,
    val ratingsCardUiState: StatisticsRatingsCardUiState?,
    val showHint: Boolean,
)

data class StatisticsCurrentMonthUiState(
    val totalPracticeDuration: Duration,
    val averageDurationPerSession: Duration,
    val breakDurationPerHour: Duration,
    val averageRatingPerSession: Float,
)

data class StatisticsSessionsCardUiState(
    val lastSevenDayPracticeDuration: List<PracticeDurationPerDay>,
    val totalPracticeDuration: Duration,
)

data class StatisticsGoalsCardUiState(
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

    private val sessions = sessionsUseCases.getAll().stateIn(
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
        // If there are no sessions return null
        if (sessions.isEmpty()) {
            return@map null
        }

        // Filter for sessions of the current month
        val currentSpecificMonth = timeProvider.now().specificMonth
        val currentMonthSessions = sessions.filter { session ->
            session.startTimestamp.specificMonth == currentSpecificMonth
        }

        // Calculate the statistics
        val totalPracticeDuration = currentMonthSessions.sumOf { (_, sections) ->
            sections.sumOf { (section, _) -> section.duration.inWholeSeconds }
        }.seconds
        val averageDurationPerSession = currentMonthSessions.size.let {
            if (it == 0) 0.seconds else totalPracticeDuration / it
        }
        val breakDurationPerHour = currentMonthSessions.sumOf { (session, _) ->
            session.breakDuration.inWholeSeconds
        }.seconds
        val averageRatingPerSession = currentMonthSessions.size.let {
            if (it == 0) {
                0f
            } else {
                currentMonthSessions.sumOf { (session, _) ->
                    session.rating
                }.toFloat() / it
            }
        }

        // Return the UI state
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

    private var _noSessionsCard = true

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sessionsCardUiState = sessions.flatMapLatest { sessions ->
        // If there are no sessions return (no sessions card)
        if (sessions.isEmpty()) {
            _noSessionsCard = true
            return@flatMapLatest flow { emit(null) }
        }

        // Compute timestamps for the last seven days
        val lastSevenDays = (0L..6L).reversed().map { dayOffset ->
            timeProvider.getStartOfDay().minusDays(dayOffset)
        }

        // Filter sessions for the last seven days
        val sessionsInLastSevenDays = sessions.filter { session ->
            session.startTimestamp > lastSevenDays.first()
        }

        // Group sessions by day of the week
        val groupedSessions = sessionsInLastSevenDays.groupBy { session ->
            getDayIndexOfWeek(session.startTimestamp)
        }

        // Compute the practice duration per day
        val lastSevenDayPracticeDuration = lastSevenDays.map { day ->
            val dayIndex = getDayIndexOfWeek(day)
            PracticeDurationPerDay(
                day = weekIndexToName(dayIndex)[0].toString(),
                duration = (
                    groupedSessions[dayIndex]?.sumOf { (_, sections) ->
                        sections.sumOf { (section, _) -> section.duration.inWholeSeconds }
                    } ?: 0
                    ).seconds
            )
        }

        // Compute the total practice duration for the last seven days
        val totalPracticeDuration = lastSevenDayPracticeDuration.sumOf {
            it.duration.inWholeSeconds
        }.seconds

        // Return the UI state as a flow
        flow {
            // If there was no sessions card before (i.e. the first time the screen is rendered),
            // show a placeholder card with zero durations for a short duration, before showing
            // the actual data. This triggers the card animation.
            if (_noSessionsCard) {
                // Emit the empty placeholder card
                emit(
                    StatisticsSessionsCardUiState(
                        lastSevenDayPracticeDuration = lastSevenDays.map { PracticeDurationPerDay() },
                        totalPracticeDuration = totalPracticeDuration,
                    )
                )
                delay(350)
                _noSessionsCard = false
            }

            // Emit the actual data
            emit(
                StatisticsSessionsCardUiState(
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

    private var _noGoalsCard = true

    @OptIn(ExperimentalCoroutinesApi::class)
    private val goalsCardUiState = lastFiveCompletedGoals.flatMapLatest { lastFiveGoals ->
        // If there are no goals return (no goals card)
        if (lastFiveGoals.isEmpty()) {
            _noGoalsCard = true
            return@flatMapLatest flow { emit(null) }
        }

        // Compute the goal statistics
        val successRate = lastFiveGoals.count {
            it.progress >= it.instance.target
        } to lastFiveGoals.size

        val lastGoalsDisplayData = lastFiveGoals.reversed().map {
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

        // Return the UI state as a flow
        flow {
            // If there was no goals card before (i.e. the first time the screen is rendered),
            // show a placeholder card with zero progress for a short duration, before showing
            // the actual data. This triggers the card animation.
            if (_noGoalsCard) {
                // Emit the empty placeholder
                emit(StatisticsGoalsCardUiState(
                    successRate = null,
                    lastGoalsDisplayData = lastFiveGoals.map { GoalCardGoalDisplayData() }
                ))
                delay(350)
                _noGoalsCard = false
            }

            // Emit the actual data
            emit(
                StatisticsGoalsCardUiState(
                    successRate = successRate,
                    lastGoalsDisplayData = lastGoalsDisplayData
            ))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private var _noRatingsCard = true

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ratingsCardUiState = sessions.flatMapLatest { sessions ->
        // If there are no ratings return (no ratings card)
        if (sessions.isEmpty()) {
            _noRatingsCard = true
            return@flatMapLatest flow { emit(null) }
        }

        // Map the ratings to the number of ratings from one to five
        val numOfRatingsFromOneToFive = sessions.groupBy { (session, _) ->
            session.rating
        }.let { ratingToSessions ->
            (1..5).map { rating ->
                ratingToSessions[rating]?.size ?: 0
            }
        }

        // Return the UI state as a flow
        flow {
            // If there was no ratings card before (i.e. the first time the screen is rendered),
            // show a placeholder card with no ratings for a short duration, before showing
            // the actual data. This triggers the card animation.
            if (_noRatingsCard) {
                // Emit the empty placeholder
                emit(StatisticsRatingsCardUiState((1..5).map { 0 }))
                delay(350)
                _noRatingsCard = false
            }

            // Emit the actual data
            emit(StatisticsRatingsCardUiState(numOfRatingsFromOneToFive))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(500),
        initialValue = null
    )

    val uiState = combine(
        currentMonthUiState,
        sessionsCardUiState,
        goalsCardUiState,
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
        started = SharingStarted.WhileSubscribed(500),
        initialValue = StatisticsUiState(
            contentUiState = StatisticsContentUiState(
                currentMonthUiState = currentMonthUiState.value,
                practiceDurationCardUiState = sessionsCardUiState.value,
                goalCardUiState = goalsCardUiState.value,
                ratingsCardUiState = ratingsCardUiState.value,
                showHint = true
            ),
        )
    )
}
