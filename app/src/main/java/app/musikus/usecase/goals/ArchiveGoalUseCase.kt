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