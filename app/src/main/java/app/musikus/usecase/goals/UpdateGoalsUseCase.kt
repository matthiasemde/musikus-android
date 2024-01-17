package app.musikus.usecase.goals

import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.Nullable
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.repository.GoalRepository
import app.musikus.utils.TimeProvider

class UpdateGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val archiveGoals: ArchiveGoalsUseCase,
    private val timeProvider: TimeProvider,
) {
    private suspend fun renewInstance(
        outdatedInstanceWithDescription: GoalInstanceWithDescription,
    ) {
        val description = outdatedInstanceWithDescription.description
        val outdatedInstance = outdatedInstanceWithDescription.instance

        val outdatedInstanceEndTimestamp =
            outdatedInstanceWithDescription.endOfInstanceInLocalTimezone

        // perform the update in a transaction
        goalRepository.withTransaction {
            if (description.paused) {
                // if the old instance belonged to a paused goal delete it...
                goalRepository.deletePausedInstance(outdatedInstance.id)
            } else {
                // otherwise mark it as renewed by setting its endTimestamp
                goalRepository.updateGoalInstance(
                    outdatedInstance.id,
                    GoalInstanceUpdateAttributes(
                        endTimestamp = Nullable(outdatedInstanceEndTimestamp),
                    )
                )
            }

            // insert a new instance with the same target and description as the old one
            // the start timestamp is the end timestamp of the old instance
            goalRepository.addNewInstance(
                GoalInstanceCreationAttributes(
                    goalDescriptionId = outdatedInstance.goalDescriptionId,
                    startTimestamp = outdatedInstanceEndTimestamp,
                    target = outdatedInstance.target
                )
            )
        }
    }

    suspend operator fun invoke() {
        var lastOutdatedGoals : List<GoalInstanceWithDescription>? = null

        // if there are no more outdated goals, we are done
        while(lastOutdatedGoals == null || lastOutdatedGoals.isNotEmpty()) {
            goalRepository.withTransaction {
                val outdatedGoals = goalRepository
                    .getLatestInstances()
                    .filter {
                        timeProvider.now() >= it.endOfInstanceInLocalTimezone
                    }

                // if the list of outdated goals doesn't change, we are stuck in an infinite loop
                if (outdatedGoals == lastOutdatedGoals) {
                    throw IllegalStateException("Stuck in infinite loop while updating goals")
                }

                val (archivedOutdatedGoals, notArchivedOutdatedGoals) = outdatedGoals.partition {
                    it.description.archived
                }

                // outdated and archived goals are finalized by setting their endTimestamp
                for (goal in archivedOutdatedGoals) {
                    goalRepository.updateGoalInstance(
                        goal.instance.id,
                        GoalInstanceUpdateAttributes(
                            endTimestamp = Nullable(goal.endOfInstanceInLocalTimezone)
                        )
                    )
                }

                // the other outdated goals are renewed if they are repeatable
                // or archived if they are one shot goals
                notArchivedOutdatedGoals.forEach { goal ->
                    val description = goal.description

                    if(description.repeat) {
                        renewInstance(goal)
                    }
                    else {
                        archiveGoals(listOf(description.id))
                    }
                }

                lastOutdatedGoals = outdatedGoals
            }
        }
    }
}