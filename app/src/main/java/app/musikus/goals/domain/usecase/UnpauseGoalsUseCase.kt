/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.goals.data.GoalRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class UnpauseGoalsUseCase(
    private val goalRepository: GoalRepository
) {

    suspend operator fun invoke(
        goalDescriptionIds: List<UUID>
    ) {
        val uniqueGoalDescriptionIds = goalDescriptionIds.distinct()

        val goals = goalRepository.allGoals.first().filter { it.description.id in uniqueGoalDescriptionIds }

        val missingGoalIds = uniqueGoalDescriptionIds - goals.map { it.description.id }.toSet()
        if(missingGoalIds.isNotEmpty()) {
            throw IllegalArgumentException("Could not find goal(s) with descriptionId: $missingGoalIds")
        }

        val archivedGoals = goals.filter { it.description.archived }
        if(archivedGoals.isNotEmpty()) {
            throw IllegalArgumentException("Cannot unpause archived goals: ${archivedGoals.map { it.description.id }}")
        }

        val nonPausedGoals = goals.filter { !it.description.paused }
        if(nonPausedGoals.isNotEmpty()) {
            throw IllegalArgumentException("Cannot unpause goals that aren't paused: ${nonPausedGoals.map { it.description.id }}")
        }

        goalRepository.updateGoalDescriptions(
            goalDescriptionIds.map { it to GoalDescriptionUpdateAttributes(paused = false) }
        )
    }
}