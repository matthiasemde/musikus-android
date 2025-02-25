/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger, Matthias Emde
 */

package app.musikus.activesession.domain.usecase

data class ActiveSessionUseCases(
    val getState: GetActiveSessionStateUseCase,
    val selectItem: SelectItemUseCase,
    val deleteSection: DeleteSectionUseCase,
    val pause: PauseActiveSessionUseCase,
    val resume: ResumeActiveSessionUseCase,
    val computeTotalPracticeDuration: ComputeTotalPracticeDurationUseCase,
    val computeRunningItemDuration: ComputeRunningItemDurationUseCase,
    val getRunningItem: GetRunningItemUseCase,
    val getCompletedSections: GetCompletedSectionsUseCase,
    val computeOngoingPauseDuration: ComputeOngoingPauseDurationUseCase,
    val getStartTime: GetStartTimeUseCase,
    val getFinalizedSession: GetFinalizedSessionUseCase,
    val reset: ResetSessionUseCase,
    val isSessionPaused: IsSessionPausedUseCase,
    val isSessionRunning: IsSessionRunningUseCase,
    val getSessionStatus: GetSessionStatusUseCase,
)
