/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

data class GoalsUseCases(
    val getAll: GetAllGoalsUseCase,
    val add: AddGoalUseCase,
    val pause: PauseGoalsUseCase,
    val unpause: UnpauseGoalsUseCase,
    val archive: ArchiveGoalsUseCase,
    val update: UpdateGoalsUseCase,
    val edit: EditGoalUseCase,
    val delete: DeleteGoalsUseCase,
    val restore: RestoreGoalsUseCase,
    val selectSortMode: SelectGoalSortModeUseCase,
)