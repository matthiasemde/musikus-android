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
    val getRunningItem: GetRunningItemUseCase,
    val getCompletedSections: GetCompletedSectionsUseCase,
    val getStartTime: GetStartTimeUseCase,
    val getFinalizedSession: GetFinalizedSessionUseCase,
    val isSessionPaused: IsSessionPausedUseCase,
    val isSessionRunning: IsSessionRunningUseCase,
    val getSessionStatus: GetSessionStatusUseCase,

    /**
     * Use case to retrieve the index of the app intro dialog for the sessions screen.
     * This index is used to determine which dialog should be shown to the user next.
     *
     * @return A flow that emits the index of the app intro dialog for the sessions screen.
     */
    val getAppIntroDialogIndex: GetAppIntroDialogIndexUseCase,

    val selectItem: SelectItemUseCase,
    val deleteSection: DeleteSectionUseCase,
    val pause: PauseActiveSessionUseCase,
    val resume: ResumeActiveSessionUseCase,
    val computeTotalPracticeDuration: ComputeTotalPracticeDurationUseCase,
    val computeRunningItemDuration: ComputeRunningItemDurationUseCase,
    val computeOngoingPauseDuration: ComputeOngoingPauseDurationUseCase,
    val reset: ResetSessionUseCase,

    /**
     * Use case to set the index of the app intro dialog for the sessions screen.
     * This is used to update the user's progress through the app intro dialogs.
     *
     * @param index The new index to set for the app intro dialog.
     */
    val setAppIntroDialogIndex: SetAppIntroDialogIndexUseCase,
)
