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
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository
import app.musikus.utils.getCurrTimestamp
import app.musikus.utils.getSpecificDay
import app.musikus.utils.getSpecificMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


/**
 * Ui state data classes
 */
data class StatisticsUiState(
    val contentUiState: StatisticsContentUiState,
)
data class StatisticsContentUiState(
    val currentMonthUiState: StatisticsCurrentMonthUiState,
    val practiceDurationCardUiState: StatisticsPracticeDurationCardUiState,
    val goalCardUiState: StatisticsGoalCardUiState,
    val ratingsCardUiState: StatisticsRatingsCardUiState,
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
    val specificDay: Int,
    val duration: Int,
)

data class StatisticsGoalCardUiState(
    val lastGoals: List<GoalWithProgress>,
)

data class StatisticsRatingsCardUiState(
    val numOfRatingsFromLowestToHighest: List<Int>,
)

class StatisticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Database */
    private val database = MusikusDatabase.getInstance(application)

    /** Repositories */
    private val libraryRepository = LibraryRepository(database)
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

    private val practiceDurationCardUiState = sessions.map { sessions ->
        val lastSevenSpecificDays = (0..6).map { getSpecificDay(getCurrTimestamp()) - it }
        val lastSevenDayPracticeDuration = sessions.filter { (_, sections) ->
            sections.first().section.timestamp.let {
                getSpecificDay(it) in lastSevenSpecificDays
            }
        }.groupBy { (_, sections) ->
            getSpecificDay(sections.first().section.timestamp)
        }.map { (specificDay, sessions) ->
            PracticeDurationPerDay(
                specificDay = specificDay,
                duration = sessions.sumOf { (_, sections) ->
                    sections.sumOf { (section, _) -> section.duration }
                }
            )
        }

        val totalPracticeDuration = lastSevenDayPracticeDuration.sumOf { it.duration }

        StatisticsPracticeDurationCardUiState(
            lastSevenDayPracticeDuration = lastSevenDayPracticeDuration,
            totalPracticeDuration = totalPracticeDuration,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsPracticeDurationCardUiState(
            lastSevenDayPracticeDuration = emptyList(),
            totalPracticeDuration = 0,
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lastFiveCompletedGoalsWithProgress = lastFiveCompletedGoals.flatMapLatest { goals ->
        val sections = goals.map { goal ->
            sessionRepository.sectionsForGoal(goal).map { sections->
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
        StatisticsGoalCardUiState(lastGoals = it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsGoalCardUiState(lastGoals = emptyList())
    )

    private val ratingsCardUiState = sessions.map { sessions ->
        val numOfRatingsFromLowestToHighest = sessions.groupBy { (session, _) ->
            session.rating
        }.map { (rating, sessions) ->
            rating to sessions.size
        }.sortedBy { (rating, _) ->
            rating
        }.map { (_, numOfRatings) ->
            numOfRatings
        }

        StatisticsRatingsCardUiState(
            numOfRatingsFromLowestToHighest = numOfRatingsFromLowestToHighest,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsRatingsCardUiState(
            numOfRatingsFromLowestToHighest = emptyList(),
        )
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
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState(
            contentUiState = StatisticsContentUiState(
                currentMonthUiState = currentMonthUiState.value,
                practiceDurationCardUiState = practiceDurationCardUiState.value,
                goalCardUiState = goalCardUiState.value,
                ratingsCardUiState = ratingsCardUiState.value,
            )
        )
    )
}