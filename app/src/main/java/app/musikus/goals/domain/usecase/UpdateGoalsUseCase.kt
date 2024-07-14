/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.GoalInstanceWithDescription
import app.musikus.core.data.Nullable
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceUpdateAttributes
import app.musikus.goals.domain.GoalRepository
import app.musikus.core.domain.TimeProvider

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
            outdatedInstanceWithDescription.endOfInstanceInLocalTimezone(timeProvider)

        // perform the update in a transaction
        goalRepository.withTransaction {
            val previousInstanceId = if (description.paused) {
                // if the old instance belonged to a paused goal delete it...
                goalRepository.deletePausedInstance(outdatedInstance.id)

                // ... and return its previousInstanceId as the new previousInstanceId
                outdatedInstance.previousInstanceId
            } else {
                // otherwise mark it as renewed by setting its endTimestamp
                goalRepository.updateGoalInstance(
                    outdatedInstance.id,
                    GoalInstanceUpdateAttributes(
                        endTimestamp = Nullable(outdatedInstanceEndTimestamp),
                    )
                )

                // and return its id as the new previousInstanceId
                outdatedInstance.id
            }

            // insert a new instance with the same target and description as the old one
            // the start timestamp is the end timestamp of the old instance
            goalRepository.addNewInstance(
                GoalInstanceCreationAttributes(
                    descriptionId = outdatedInstance.descriptionId,
                    previousInstanceId = previousInstanceId,
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
                        timeProvider.now() >= it.endOfInstanceInLocalTimezone(timeProvider)
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
                            endTimestamp = Nullable(goal.endOfInstanceInLocalTimezone(timeProvider))
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