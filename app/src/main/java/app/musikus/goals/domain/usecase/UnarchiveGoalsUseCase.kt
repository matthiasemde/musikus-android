package app.musikus.goals.domain.usecase

import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.goals.data.GoalRepository
import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.first
import java.util.UUID

class UnarchiveGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val timeProvider: TimeProvider
) {

    suspend operator fun invoke(
        goalDescriptionIds: List<UUID>
    ) {
        val uniqueGoalDescriptionIds = goalDescriptionIds.distinct()

        val goals = goalRepository.allGoals.first().filter {
            it.description.id in uniqueGoalDescriptionIds
        }

        val missingGoalIds = uniqueGoalDescriptionIds - goals.map { it.description.id }.toSet()
        if(missingGoalIds.isNotEmpty()) {
            throw IllegalArgumentException("Could not find goal(s) with descriptionId(s): $missingGoalIds")
        }

        val nonArchivedGoals = goals.filter { !it.description.archived }
        if(nonArchivedGoals.isNotEmpty()) {
            throw IllegalArgumentException("Cannot unarchive goals that aren't archived: ${nonArchivedGoals.map { it.description.id }}")
        }

        goalRepository.withTransaction {
            // when unarchiving a goal which has already been finalized, insert a new instance
            for (goal in goals) {
                val latestInstance = goal.latestInstance
                if(latestInstance.endTimestamp != null) {
                    goalRepository.addNewInstance(
                        GoalInstanceCreationAttributes(
                            descriptionId = goal.description.id,
                            previousInstanceId = latestInstance.id,
                            startTimestamp = when(goal.description.periodUnit) {
                                GoalPeriodUnit.DAY -> timeProvider.getStartOfDay()
                                GoalPeriodUnit.WEEK -> timeProvider.getStartOfWeek()
                                GoalPeriodUnit.MONTH -> timeProvider.getStartOfMonth()
                            },
                            target = latestInstance.target
                        )
                    )
                }
            }

            goalRepository.updateGoalDescriptions(
                goalDescriptionIds.map { it to GoalDescriptionUpdateAttributes(archived = false) }
            )
        }
    }
}