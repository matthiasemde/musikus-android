/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-2025 Matthias Emde, Michael Prommersberger
 */

package app.musikus.sessions.domain.usecase

data class SessionsUseCases(
    val getAll: GetAllSessionsUseCase,

    /**
     * Use case to retrieve the index of the app intro dialog for the sessions screen.
     * This index is used to determine which dialog should be shown to the user next.
     *
     * @return A flow that emits the index of the app intro dialog for the sessions screen.
     */
    val getAppIntroDialogIndex: GetAppIntroDialogIndexUseCase,

    val getSessionsForDaysForMonths: GetSessionsForDaysForMonthsUseCase,
    val getInTimeframe: GetSessionsInTimeframeUseCase,
    val getById: GetSessionByIdUseCase,
    val add: AddSessionUseCase,
    val edit: EditSessionUseCase,
    val delete: DeleteSessionsUseCase,
    val restore: RestoreSessionsUseCase,

    /**
     * Use case to set the index of the app intro dialog for the sessions screen.
     * This is used to update the user's progress through the app intro dialogs.
     *
     * @param index The new index to set for the app intro dialog.
     */
    val setAppIntroDialogIndex: SetAppIntroDialogIndexUseCase,
)
