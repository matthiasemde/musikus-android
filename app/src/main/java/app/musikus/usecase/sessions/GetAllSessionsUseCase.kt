/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.sessions

import app.musikus.repository.SessionRepository
import app.musikus.utils.specificDay
import app.musikus.utils.specificMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

class GetAllSessionsUseCase(
    private val sessionsRepository: SessionRepository
) {

    operator fun invoke() : Flow<List<SessionsForDaysForMonth>> {
        return sessionsRepository.sessionsWithSectionsWithLibraryItemsOrderedSeconds.map { sessions ->
            if (sessions.isEmpty()) {
                return@map emptyList()
            }

            val sessionsForDaysForMonths = mutableListOf<SessionsForDaysForMonth>()

            // initialize variables to keep track of the current month, current day,
            // the index of its first session and the total duration of the current day
            var (currentDay, currentMonth) = sessions.first().startTimestamp.let { timestamp ->
                Pair(timestamp.specificDay, timestamp.specificMonth)
            }

            var firstSessionOfDayIndex = 0
            var totalPracticeDuration = 0.seconds

            val sessionsForDaysForMonth = mutableListOf<SessionsForDay>()

            // then loop trough all of the sessions...
            sessions.forEachIndexed { index, session ->
                // ...get the month and day...
                val sessionTimestamp = session.startTimestamp
                val (day, month) = sessionTimestamp.let { timestamp ->
                    Pair(timestamp.specificDay, timestamp.specificMonth)
                }

                val sessionPracticeDuration = session.sections.sumOf {
                    it.section.duration.inWholeSeconds
                }.seconds

                // ...and compare them to the current day first.
                if (day == currentDay) {
                    totalPracticeDuration += sessionPracticeDuration
                    return@forEachIndexed
                }

                // if it differs, create a new SessionsForDay object
                // with the respective subList of sessions
                sessionsForDaysForMonth.add(
                    SessionsForDay(
                        specificDay = currentDay,
                        totalPracticeDuration = totalPracticeDuration,
                        sessions = sessions.slice(firstSessionOfDayIndex until index)
                    )
                )

                // reset / set tracking variables appropriately
                currentDay = day
                firstSessionOfDayIndex = index
                totalPracticeDuration = sessionPracticeDuration

                // then compare the month to the current month.
                // if it differs, create a new SessionsForDaysForMonth object
                // storing the specific month along with the list of SessionsForDay objects
                if (month == currentMonth) return@forEachIndexed

                sessionsForDaysForMonths.add(
                    SessionsForDaysForMonth(
                        specificMonth = currentMonth,
                        sessionsForDays = sessionsForDaysForMonth.toList()
                    )
                )

                // set tracking variable and reset list
                currentMonth = month
                sessionsForDaysForMonth.clear()
            }

            // importantly, add the last SessionsForDaysForMonth object
            sessionsForDaysForMonth.add(
                SessionsForDay(
                    specificDay = currentDay,
                    totalPracticeDuration = totalPracticeDuration,
                    sessions = sessions.slice(firstSessionOfDayIndex until sessions.size)
                )
            )
            sessionsForDaysForMonths.add(
                SessionsForDaysForMonth(
                    specificMonth = currentMonth,
                    sessionsForDays = sessionsForDaysForMonth
                )
            )

            // finally return the list as immutable
            sessionsForDaysForMonths.toList()
        }
    }
}