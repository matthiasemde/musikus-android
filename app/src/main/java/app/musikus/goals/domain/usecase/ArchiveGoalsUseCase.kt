/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.goals.data.entities.GoalDescriptionUpdateAttributes
import app.musikus.goals.domain.GoalRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class ArchiveGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val cleanFutureGoalInstances: CleanFutureGoalInstancesUseCase,
) {

    suspend operator fun invoke(
        goalDescriptionIds: List<UUID>
    ) {
        val uniqueGoalDescriptionIds = goalDescriptionIds.distinct()

        val currentGoals = goalRepository.currentGoals.first().filter {
            it.description.description.id in uniqueGoalDescriptionIds
        }

        val missingGoalIds = uniqueGoalDescriptionIds - currentGoals.map { it.description.description.id }.toSet()
        if (missingGoalIds.isNotEmpty()) {
            throw IllegalArgumentException("Could not find goal(s) with descriptionId(s): $missingGoalIds")
        }

        val archivedGoals = currentGoals.filter { it.description.description.archived }
        if (archivedGoals.isNotEmpty()) {
            throw IllegalArgumentException(
                "Cannot archive already archived goal(s): ${archivedGoals.map { it.description.description.id }}"
            )
        }

        // clean future instances before archiving
        cleanFutureGoalInstances()

        // when archiving a paused goal, delete the current instance
        for (currentGoal in currentGoals) {
            val description = currentGoal.description.description

            // if previous instance id is zero, the goal instance is the first instance of the goal
            // and should not be deleted
            if (description.paused && currentGoal.instance.previousInstanceId != null) {
                goalRepository.deletePausedInstance(currentGoal.instance.id)
            }
        }

        goalRepository.updateGoalDescriptions(
            goalDescriptionIds.map { it to GoalDescriptionUpdateAttributes(archived = true) }
        )
    }
}
