/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

package app.musikus.usecase.activesession

data class ActiveSessionUseCases(
    val selectItem: SelectItemUseCase,
    val deleteSection: DeleteSectionUseCase,
    val pause: PauseActiveSessionUseCase,
    val resume: ResumeActiveSessionUseCase,
    val getPracticeDuration: GetTotalPracticeDurationUseCase,
    val getRunningItemDuration: GetRunningItemDurationUseCase,
    val getRunningItem: GetRunningItemUseCase,
    val getCompletedSections: GetCompletedSectionsUseCase,
    val getOngoingPauseDuration: GetOngoingPauseDurationUseCase,
    val getPausedState: GetPausedStateUseCase,
    val getStartTime: GetStartTimeUseCase,
    val getFinalizedSession: GetFinalizedSessionUseCase,
    val reset: ResetSessionUseCase,
    val isSessionRunning: IsSessionRunningUseCase,
    val getTimerState: GetSessionTimerState,
)