package app.musikus.usecase.goals

import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.repository.GoalRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class PauseGoalsUseCase(
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
            throw IllegalArgumentException("Cannot pause archived goals: ${archivedGoals.map { it.description.id }}")
        }

        val pausedGoals = goals.filter { it.description.paused }
        if(pausedGoals.isNotEmpty()) {
            throw IllegalArgumentException("Cannot pause already paused goals: ${pausedGoals.map { it.description.id }}")
        }

        goalRepository.updateGoalDescriptions(
            goalDescriptionIds.map { it to GoalDescriptionUpdateAttributes(paused = true) }
        )
    }
}