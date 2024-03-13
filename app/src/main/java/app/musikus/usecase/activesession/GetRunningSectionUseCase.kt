/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.usecase.activesession


import app.musikus.database.daos.LibraryItem
import app.musikus.utils.TimeProvider
import app.musikus.utils.minus
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GetRunningSectionUseCase (
    private val activeSessionRepository: ActiveSessionRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        at: ZonedDateTime = timeProvider.now()
    ) : Pair<LibraryItem, Duration> {
        val state = activeSessionRepository.getSessionState().first()
            ?: throw IllegalStateException("State is null. Cannot get running section!")

        val duration = if (state.isPaused) {
            if (state.currentPauseStartTimestamp == null) {
                throw IllegalStateException("CurrentPauseTimestamp is null although isPaused is true.")
            }
            state.currentPauseStartTimestamp - state.startTimestampSectionPauseCompensated
        } else {
            at - state.startTimestampSectionPauseCompensated
        }
        if (duration < 0.seconds) {
            throw IllegalStateException("Duration is negative. This should not happen.")
        }
        return Pair(state.currentSectionItem, duration)
    }
}