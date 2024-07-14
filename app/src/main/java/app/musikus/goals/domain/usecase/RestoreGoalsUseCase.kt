/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.goals.domain.GoalRepository
import java.util.UUID

class RestoreGoalsUseCase(
    private val goalRepository: GoalRepository
) {

    suspend operator fun invoke(descriptionIds: List<UUID>) {
        goalRepository.restore(descriptionIds)
    }
}