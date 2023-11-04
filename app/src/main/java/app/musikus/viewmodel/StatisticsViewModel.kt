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
import app.musikus.database.MusikusDatabase
import app.musikus.repository.GoalRepository
import app.musikus.repository.SessionRepository
import app.musikus.utils.getCurrTimestamp
import app.musikus.utils.getCurrentDayIndexOfWeek
import app.musikus.utils.getSpecificMonth
import app.musikus.utils.getStartOfDayOfWeek
import app.musikus.utils.weekIndexToName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


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
)

data class StatisticsCurrentMonthUiState(
    val totalPracticeDuration: Int,
    val averageDurationPerSession: Int,
    val breakDurationPerHour: Int,
    val averageRatingPerSession: Float,
)

data class StatisticsPracticeDurationCardUiState(
    val lastSevenDayPracticeDuration: List<PracticeDurationPerDay>,
    val totalPracticeDuration: Int,
)

data class PracticeDurationPerDay(
    val day: String,
    val duration: Int,
)

data class StatisticsGoalCardUiState(
    val lastGoals: List<GoalWithProgress>,
)

data class StatisticsRatingsCardUiState(
    val numOfRatingsFromOneToFive: List<Int>,
)

class StatisticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Database */
    private val database = MusikusDatabase.getInstance(application)

    /** Repositories */
    private val goalRepository = GoalRepository(database)
    private val sessionRepository = SessionRepository(database)

    private val sessions = sessionRepository.sessionsWithSectionsWithLibraryItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val lastFiveCompletedGoals = goalRepository.lastFiveCompletedGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    /**
     *  Composing the Ui state
     */

    private val currentMonthUiState = sessions.map { sessions ->
        if (sessions.isEmpty()) return@map null

        val currentSpecificMonth = getSpecificMonth(getCurrTimestamp())
        val currentMonthSessions = sessions.filter { (_, sections) ->
            sections.first().section.timestamp.let{
                getSpecificMonth(it) == currentSpecificMonth
            }
        }
        val totalPracticeDuration = currentMonthSessions.sumOf { (_, sections) ->
            sections.sumOf { (section, _) -> section.duration }
        }
        val averageDurationPerSession = currentMonthSessions.size.let {
            if(it == 0) 0 else totalPracticeDuration / it
        }
        val breakDurationPerHour = currentMonthSessions.sumOf { (session, _) ->
            session.breakDuration
        }
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
        initialValue = StatisticsCurrentMonthUiState(
            totalPracticeDuration = 0,
            averageDurationPerSession = 0,
            breakDurationPerHour = 0,
            averageRatingPerSession = 0f,
        )
    )

    private var _noSessionsForDurationCard = true

    @OptIn(ExperimentalCoroutinesApi::class)
    private val practiceDurationCardUiState = sessions.flatMapLatest { sessions ->
        if (sessions.isEmpty()) {
            _noSessionsForRatingCard = true
            return@flatMapLatest flow { emit(null) }
        }

        val lastSevenDays = (0..6).reversed().map { dayOffset ->
            (getCurrentDayIndexOfWeek() - dayOffset).let {
                getStartOfDayOfWeek(
                    dayIndex = (it-1).mod(7).toLong() + 1,
                    weekOffset = if (it > 0) 0 else -1
                )
            }
        }

        val groupedSessions = sessions.filter { (_, sections) ->
            sections.first().section.timestamp > lastSevenDays.first().toEpochSecond()
        }.groupBy { (_, sections) ->
            getCurrentDayIndexOfWeek(sections.first().section.timestamp)
        }

        val lastSevenDayPracticeDuration = lastSevenDays.map { day ->
            val dayIndex = getCurrentDayIndexOfWeek(day.toEpochSecond())
            PracticeDurationPerDay(
                day = weekIndexToName(dayIndex)[0].toString(),
                duration = groupedSessions[dayIndex]?.sumOf { (_, sections) ->
                    sections.sumOf { (section, _) -> section.duration }
                } ?: 0
            )
        }

        val totalPracticeDuration = lastSevenDayPracticeDuration.sumOf { it.duration }

        flow {
            if (_noSessionsForDurationCard) {
                emit(StatisticsPracticeDurationCardUiState(
                    lastSevenDayPracticeDuration = lastSevenDayPracticeDuration.map { PracticeDurationPerDay(
                        day = it.day,
                        duration = 0
                    ) },
                    totalPracticeDuration = totalPracticeDuration,
                ))
                _noSessionsForDurationCard = false
                delay(350)
            }
            emit(StatisticsPracticeDurationCardUiState(
                lastSevenDayPracticeDuration = lastSevenDayPracticeDuration,
                totalPracticeDuration = totalPracticeDuration,
            ))
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lastFiveCompletedGoalsWithProgress = lastFiveCompletedGoals.flatMapLatest { goals ->
        val sections = goals.map { goal ->
            sessionRepository.sectionsForGoal(goal).map { sections ->
                goal to sections
            }
        }

        combine(sections) { combinedGoalsWithSections ->
            combinedGoalsWithSections.map { (goal, sections) ->
                GoalWithProgress(
                    goal = goal,
                    progress = sections.sumOf { section ->
                        section.duration
                    }
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val goalCardUiState = lastFiveCompletedGoalsWithProgress.map {
        if (it.isEmpty()) return@map null

        StatisticsGoalCardUiState(lastGoals = it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsGoalCardUiState(lastGoals = emptyList())
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
                _noSessionsForRatingCard = false
                delay(350)
            }
            emit(StatisticsRatingsCardUiState(numOfRatingsFromOneToFive))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(500),
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
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(500),
        initialValue = StatisticsUiState(
            contentUiState = StatisticsContentUiState(
                currentMonthUiState = currentMonthUiState.value,
                practiceDurationCardUiState = practiceDurationCardUiState.value,
                goalCardUiState = goalCardUiState.value,
                ratingsCardUiState = ratingsCardUiState.value,
            ),
        )
    )
}