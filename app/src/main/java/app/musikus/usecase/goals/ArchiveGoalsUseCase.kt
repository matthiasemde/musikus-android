/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.repository.GoalRepository
import java.util.UUID

class ArchiveGoalsUseCase(
    private val goalRepository: GoalRepository
) {

        suspend operator fun invoke(
            goalDescriptionIds: List<UUID>
        ) {
            goalRepository.updateGoalDescriptions(
                goalDescriptionIds.map { it to GoalDescriptionUpdateAttributes(archived = true) }
            )
        }
}