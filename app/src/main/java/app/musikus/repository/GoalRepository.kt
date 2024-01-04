/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.repository

import androidx.room.Transaction
import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.Nullable
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalDescriptionDao
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.GoalInstanceDao
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.time.Duration

interface GoalRepository {
    val timeProvider: TimeProvider
    val currentGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
    val allGoals: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
    val lastFiveCompletedGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    /** Mutators */
    /** Add */
    suspend fun add(
        descriptionCreationAttributes: GoalDescriptionCreationAttributes,
        instanceCreationAttributes: GoalInstanceCreationAttributes,
        libraryItems: List<LibraryItem>?,
    )

    /** Edit */
    suspend fun editGoalTarget(
        goal: GoalInstance,
        newTarget: Duration,
    )


    /** Pause / Unpause */
    suspend fun pause(goal: GoalDescription)
    suspend fun pause(goals: List<GoalDescription>)

    suspend fun unpause(goal: GoalDescription)
    suspend fun unpause(goals: List<GoalDescription>)

    /** Archive / Unarchive */
    suspend fun archive(goal: GoalDescription)
    suspend fun archive(goals: List<GoalDescription>)

    suspend fun unarchive(goal: GoalDescription)
    suspend fun unarchive(goals: List<GoalDescription>)

    /** Delete / Restore */
    suspend fun delete(goal: GoalDescription)
    suspend fun delete(goals: List<GoalDescription>)

    suspend fun restore(goal: GoalDescription)
    suspend fun restore(goals: List<GoalDescription>)

    /** Clean */
    suspend fun clean()

    /** Update Goals */
    suspend fun updateGoals()
}

class GoalRepositoryImpl(
    private val goalInstanceDao : GoalInstanceDao,
    private val goalDescriptionDao : GoalDescriptionDao,
    override val timeProvider: TimeProvider
) : GoalRepository {

    override val currentGoals = goalInstanceDao.getCurrent()
    override val allGoals = goalDescriptionDao.getAllWithInstancesAndLibraryItems()
    override val lastFiveCompletedGoals = goalInstanceDao.getLastNCompleted(5)


    /** Mutators */

    /** Add */
    override suspend fun add(
        descriptionCreationAttributes: GoalDescriptionCreationAttributes,
        instanceCreationAttributes: GoalInstanceCreationAttributes,
        libraryItems: List<LibraryItem>?,
    ) {

        goalDescriptionDao.insert(
            description = GoalDescriptionModel(
                type = descriptionCreationAttributes.type,
                repeat = descriptionCreationAttributes.repeat,
                periodInPeriodUnits = descriptionCreationAttributes.periodInPeriodUnits,
                periodUnit = descriptionCreationAttributes.periodUnit,
            ),
            instanceCreationAttributes = instanceCreationAttributes,
            libraryItemIds = libraryItems?.map { it.id },
        )
    }

    private suspend fun renewInstance(
        outdatedInstanceWithDescription: GoalInstanceWithDescription,
    ) {
        val description = outdatedInstanceWithDescription.description
        val outdatedInstance = outdatedInstanceWithDescription.instance

        val outdatedInstanceEndTimestamp =
            outdatedInstanceWithDescription.endOfInstanceInLocalTimezone

        // perform the update in a transaction
        goalInstanceDao.transaction {
            // insert a new instance with the same target and description as the old one
            // the start timestamp is the end timestamp of the old instance
            goalInstanceDao.insert(
                outdatedInstance.goalDescriptionId,
                GoalInstanceCreationAttributes(
                    startTimestamp = outdatedInstanceEndTimestamp,
                    target = outdatedInstance.target
                )
            )

            if (description.paused) {
                // if the old instance belonged to a paused goal delete it...
                goalInstanceDao.deletePausedGoalInstance(outdatedInstance.id)
            } else {
                // otherwise mark it as renewed by setting its endTimestamp
                goalInstanceDao.update(
                    outdatedInstance.id,
                    GoalInstanceUpdateAttributes(
                        endTimestamp = Nullable(outdatedInstanceEndTimestamp),
                    )
                )
            }
        }
    }


    /** Edit */
    override suspend fun editGoalTarget(
        goal: GoalInstance,
        newTarget: Duration,
    ) {
        // before editing we need to remove possible future instances
        // which would still have the outdated target
        cleanFutureInstances(listOf(goal.goalDescriptionId))

        goalInstanceDao.update(
            goal.id,
            GoalInstanceUpdateAttributes(target = newTarget)
        )
    }


    /** Pause / Unpause */
    @Transaction
    override suspend fun pause(goal: GoalDescription) {
        pause(listOf(goal))
    }

    @Transaction
    override suspend fun pause(goals: List<GoalDescription>) {
        // before pausing we need to clean for possible future instances
        cleanFutureInstances(goals.map { it.id })

        goalDescriptionDao.update(
            goals.map {
                it.id to GoalDescriptionUpdateAttributes(paused = true)
            },
        )
    }

    @Transaction
    override suspend fun unpause(goal: GoalDescription) {
        unpause(listOf(goal))
    }

    @Transaction
    override suspend fun unpause(goals: List<GoalDescription>) {
        goalDescriptionDao.update(
            goals.map {
                it.id to GoalDescriptionUpdateAttributes(paused = false)
            }
        )
        updateGoals()
    }


    /** Archive / Unarchive */

    override suspend fun archive(goal: GoalDescription) {
        archive(listOf(goal))
    }

    override suspend fun archive(goals: List<GoalDescription>) {
        val descriptionIds = goals.map { it.id }

        // before archiving we need to check if there are instances in the future
        cleanFutureInstances(descriptionIds)

        goalDescriptionDao.transaction {
            goalDescriptionDao.update(descriptionIds.map {
                it to GoalDescriptionUpdateAttributes(archived = true)
            })
            val latestInstancesWithDescription = goalInstanceDao.getLatest(descriptionIds)

            if(latestInstancesWithDescription.size != descriptionIds.size) {
                throw IllegalStateException("Not all goals have instances")
            }

            goalInstanceDao.update(
                latestInstancesWithDescription.map {
                    it.instance.id to GoalInstanceUpdateAttributes(
                        endTimestamp = Nullable(it.endOfInstanceInLocalTimezone)
                    )
                }
            )
        }
    }

    override suspend fun unarchive(goal: GoalDescription) {
        unarchive(listOf(goal))
    }

    override suspend fun unarchive(goals: List<GoalDescription>) {
        val descriptionIds = goals.map { it.id }

        goalDescriptionDao.transaction {
            goalDescriptionDao.update(descriptionIds.map {
                it to GoalDescriptionUpdateAttributes(archived = false)
            })
            val latestInstancesWithDescription = goalInstanceDao.getLatest(descriptionIds)

            if(latestInstancesWithDescription.size != descriptionIds.toSet().size) {
                throw IllegalStateException("Not all goals have instances")
            }

            goalInstanceDao.update(
                latestInstancesWithDescription.map {
                    it.instance.id to GoalInstanceUpdateAttributes(
                        endTimestamp = Nullable(null)
                    )
                }
            )
        }
    }


    /** Delete / Restore */

    override suspend fun delete(goal: GoalDescription) {
        delete(listOf(goal))
    }

    override suspend fun delete(goals: List<GoalDescription>) {
        goalDescriptionDao.delete(goals.map { it.id })
    }

    override suspend fun restore(goal: GoalDescription) {
        restore(listOf(goal))
    }

    override suspend fun restore(goals: List<GoalDescription>) {
        goalDescriptionDao.restore(goals.map { it.id })
    }

    /** Clean */

    override suspend fun clean() {
        goalDescriptionDao.clean()
    }


    /**
     * Utility functions
     * */


    /** Update Goals */
    override suspend fun updateGoals() {
        var lastOutdatedGoals : List<GoalInstanceWithDescription>? = null

        // if there are no more outdated goals, we are done
        while(lastOutdatedGoals == null || lastOutdatedGoals.isNotEmpty()) {
            goalInstanceDao.transaction {
                val outdatedGoals = goalInstanceDao
                    .getUpdateCandidates()
                    .filter {
                        timeProvider.now() >= it.endOfInstanceInLocalTimezone
                    }

                // if the list of outdated goals doesn't change, we are stuck in an infinite loop
                if (outdatedGoals == lastOutdatedGoals) {
                    throw IllegalStateException("Stuck in infinite loop while updating goals")
                }

                // while there are still outdated goals, keep looping and adding new ones
                outdatedGoals.forEach { outdatedGoal ->
                    val description = outdatedGoal.description
                    if (description.repeat) {
                        // if the goal is repeatable, renew the instance
                        renewInstance(outdatedGoal)
                    } else if (!description.archived) {
                        // one shot goals are archived after they are outdated
                        archive(description)
                    }
                }
                lastOutdatedGoals = outdatedGoals
            }
        }
    }

    private suspend fun cleanFutureInstances(descriptionIds: List<UUID>) {
        var lastFutureInstances : List<GoalInstance>? = null

        while(lastFutureInstances == null || lastFutureInstances.isNotEmpty()) {
            goalInstanceDao.transaction {
                val futureInstances = goalInstanceDao
                    .getLatest(descriptionIds)
                    .map { it.instance }
                    .filter {
                        it.startTimestamp > timeProvider.now()
                    }

                if(futureInstances == lastFutureInstances) {
                    throw IllegalStateException("Stuck in infinite loop while cleaning future instances")
                }

                goalInstanceDao.deleteFutureGoalInstances(
                    futureInstances.map { it.id }
                )

                lastFutureInstances = futureInstances
            }
        }
    }
}