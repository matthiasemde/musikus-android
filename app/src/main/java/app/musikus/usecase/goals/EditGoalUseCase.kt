package app.musikus.usecase.goals

import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.database.entities.InvalidGoalDescriptionException
import app.musikus.database.entities.InvalidGoalInstanceException
import app.musikus.repository.GoalRepository
import java.lang.IllegalStateException
import java.util.UUID

class EditGoalUseCase(
    private val goalRepository: GoalRepository,
    private val cleanFutureGoalInstancesUseCase: CleanFutureGoalInstancesUseCase
) {

    suspend operator fun invoke(
        descriptionId: UUID,
        descriptionUpdateAttributes: GoalDescriptionUpdateAttributes? = null,
        instanceUpdateAttributes: GoalInstanceUpdateAttributes? = null,
    ) {

        if(!goalRepository.existsDescription(descriptionId)) {
            throw IllegalArgumentException("Goal with id $descriptionId does not exist")
        }

        if (descriptionUpdateAttributes?.archived != null) {
            throw IllegalArgumentException("Use the archive goal use case to archive goals")
        }

        if (descriptionUpdateAttributes?.paused != null) {
            throw IllegalArgumentException("Use the pause goals use case to pause goals")
        }

        if (descriptionUpdateAttributes?.customOrder != null) {
            throw NotImplementedError("Custom order is not implemented yet")
        }

        // Before we can update the goal instance, we need to make sure that there are no future instances
        cleanFutureGoalInstancesUseCase()

        val latestInstance = try {
            goalRepository.getLatestInstances().single {
                it.description.id == descriptionId
            }.instance
        } catch (e: NoSuchElementException) {
            throw IllegalStateException("Goal has no instances")
        }

        if (instanceUpdateAttributes?.endTimestamp != null) {
            throw InvalidGoalDescriptionException("End timestamp cannot be set manually")
        }

        if(
            instanceUpdateAttributes?.target != null &&
            !(
                instanceUpdateAttributes.target.isFinite() &&
                instanceUpdateAttributes.target.inWholeSeconds > 0
            )
        ) {
            throw InvalidGoalInstanceException("Target must be finite and greater than 0")
        }

        // finally, execute the update in a transaction
        goalRepository.withTransaction {
            if (descriptionUpdateAttributes != null) {
                goalRepository.updateGoalDescriptions(
                    listOf(descriptionId to descriptionUpdateAttributes)
                )
            }

            if (instanceUpdateAttributes != null) {
                goalRepository.updateGoalInstance(
                    id = latestInstance.id,
                    updateAttributes = instanceUpdateAttributes
                )
            }
        }
    }
}