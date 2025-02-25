/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.domain.usecase

data class SessionsUseCases(
    val getAll: GetAllSessionsUseCase,
    val getSessionsForDaysForMonths: GetSessionsForDaysForMonthsUseCase,
    val getInTimeframe: GetSessionsInTimeframeUseCase,
    val getById: GetSessionByIdUseCase,
    val add: AddSessionUseCase,
    val edit: EditSessionUseCase,
    val delete: DeleteSessionsUseCase,
    val restore: RestoreSessionsUseCase,
)
