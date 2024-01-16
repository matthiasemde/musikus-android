/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.repository

import androidx.room.withTransaction
import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.time.Duration

interface GoalRepository {
    val currentGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
    val allGoals: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
    val lastFiveCompletedGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    suspend fun getLatestInstances(): List<GoalInstanceWithDescription>

    /** Mutators */
    /** Add */
    suspend fun add(
        descriptionCreationAttributes: GoalDescriptionCreationAttributes,
        instanceCreationAttributes: GoalInstanceCreationAttributes,
        libraryItemIds: List<UUID>?,
    )

    /** Edit */

    suspend fun updateGoalDescriptions(
        idsWithUpdateAttributes: List<Pair<UUID, GoalDescriptionUpdateAttributes>>,
    )

    suspend fun editGoalTarget(
        goal: GoalInstance,
        newTarget: Duration,
    )


    /** Pause / Unpause */
//    suspend fun pause(goal: GoalDescription)
//    suspend fun pause(goals: List<GoalDescription>)
//
//    suspend fun unpause(goal: GoalDescription)
//    suspend fun unpause(goals: List<GoalDescription>)

    /** Archive / Unarchive */
    suspend fun archive(goal: GoalDescription)
    suspend fun archive(goals: List<GoalDescription>)

    suspend fun unarchive(goal: GoalDescription)
    suspend fun unarchive(goals: List<GoalDescription>)

    /** Delete / Restore */
    suspend fun delete(goals: List<GoalDescription>)

    suspend fun restore(goals: List<GoalDescription>)

    suspend fun deleteFutureInstances(instanceIds: List<UUID>)

    /** Clean */
    suspend fun clean()

    /** Transaction */
    suspend fun withTransaction(block: suspend () -> Unit)

    /** Update Goals */
    suspend fun updateGoals()
}

class GoalRepositoryImpl(
    private val database: MusikusDatabase,
) : GoalRepository {

    private val instanceDao = database.goalInstanceDao
    private val descriptionDao = database.goalDescriptionDao
    private val timeProvider = database.timeProvider

    override val currentGoals = instanceDao.getCurrent()
    override val allGoals = descriptionDao.getAllWithInstancesAndLibraryItems()
    override val lastFiveCompletedGoals = instanceDao.getLastNCompleted(5)

    override suspend fun getLatestInstances(): List<GoalInstanceWithDescription> {
        return instanceDao.getLatest()
    }


    /** Mutators */

    /** Add */
    override suspend fun add(
        descriptionCreationAttributes: GoalDescriptionCreationAttributes,
        instanceCreationAttributes: GoalInstanceCreationAttributes,
        libraryItemIds: List<UUID>?,
    ) {
        descriptionDao.insert(
            descriptionCreationAttributes = descriptionCreationAttributes,
            instanceCreationAttributes = instanceCreationAttributes,
            libraryItemIds = libraryItemIds,
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
        instanceDao.transaction {
            if (description.paused) {
                // if the old instance belonged to a paused goal delete it...
                instanceDao.deletePausedInstance(outdatedInstance.id)
            } else {
                // otherwise mark it as renewed by setting its endTimestamp
                instanceDao.update(
                    outdatedInstance.id,
                    GoalInstanceUpdateAttributes(
                        endTimestamp = Nullable(outdatedInstanceEndTimestamp),
                    )
                )
            }

            // insert a new instance with the same target and description as the old one
            // the start timestamp is the end timestamp of the old instance
            instanceDao.insert(
                GoalInstanceCreationAttributes(
                    goalDescriptionId = outdatedInstance.goalDescriptionId,
                    startTimestamp = outdatedInstanceEndTimestamp,
                    target = outdatedInstance.target
                )
            )
        }
    }


    /** Edit */

    override suspend fun updateGoalDescriptions(
        idsWithUpdateAttributes: List<Pair<UUID, GoalDescriptionUpdateAttributes>>
    ) {
        descriptionDao.update(idsWithUpdateAttributes)
    }

    override suspend fun editGoalTarget(
        goal: GoalInstance,
        newTarget: Duration,
    ) {
        // before editing we need to remove possible future instances
        // which would still have the outdated target
        cleanFutureInstances()

        instanceDao.update(
            goal.id,
            GoalInstanceUpdateAttributes(target = newTarget)
        )
    }


    /** Archive / Unarchive */

    override suspend fun archive(goal: GoalDescription) {
        // TODO handle archiving paused goals: simply delete current instance
        archive(listOf(goal))
    }

    override suspend fun archive(goals: List<GoalDescription>) {
        val descriptionIds = goals.map { it.id }

        // before archiving we need to clean possible future instances
        cleanFutureInstances()

        descriptionDao.update(descriptionIds.map {
            it to GoalDescriptionUpdateAttributes(archived = true)
        })
    }

    override suspend fun unarchive(goal: GoalDescription) {
        unarchive(listOf(goal))
    }

    override suspend fun unarchive(goals: List<GoalDescription>) {
        val descriptionIds = goals.map { it.id }

        descriptionDao.transaction {

            // make sure there is an open instance for each goal
            for (descriptionId in descriptionIds) {
                if(!instanceDao.getForDescription(descriptionId).any { it.endTimestamp == null }) {
                    throw IllegalArgumentException("Cannot unarchive goal without any open instances")
                }
            }

            descriptionDao.update(descriptionIds.map {
                it to GoalDescriptionUpdateAttributes(archived = false)
            })
        }
    }


    /** Delete / Restore */

    override suspend fun delete(goals: List<GoalDescription>) {
        descriptionDao.delete(goals.map { it.id })
    }

    override suspend fun restore(goals: List<GoalDescription>) {
        descriptionDao.restore(goals.map { it.id })
    }

    override suspend fun deleteFutureInstances(instanceIds: List<UUID>) {
        instanceDao.deleteFutureInstances(instanceIds)
    }

    /** Clean */

    override suspend fun clean() {
        descriptionDao.clean()
    }

    /** Transaction */

    override suspend fun withTransaction(block: suspend () -> Unit) {
        return database.withTransaction(block)
    }

    /**
     * Utility functions
     * */


    /** Update Goals */
    override suspend fun updateGoals() {
        var lastOutdatedGoals : List<GoalInstanceWithDescription>? = null

        // if there are no more outdated goals, we are done
        while(lastOutdatedGoals == null || lastOutdatedGoals.isNotEmpty()) {
            instanceDao.transaction {
                val outdatedGoals = instanceDao
                    .getLatest()
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
                instanceDao.update(
                    archivedOutdatedGoals.map {
                        it.instance.id to GoalInstanceUpdateAttributes(
                            endTimestamp = Nullable(it.endOfInstanceInLocalTimezone)
                        )
                    }
                )

                // the other outdated goals are renewed if they are repeatable
                // or archived if they are one shot goals
                notArchivedOutdatedGoals.forEach { goal ->
                    val description = goal.description

                    if(description.repeat) {
                        renewInstance(goal)
                    }
                    else {
                        archive(description)
                    }
                }

                lastOutdatedGoals = outdatedGoals
            }
        }
    }

    private suspend fun cleanFutureInstances() {
        var lastFutureInstances : List<GoalInstance>? = null

        while(lastFutureInstances == null || lastFutureInstances.isNotEmpty()) {
            instanceDao.transaction {
                val futureInstances = instanceDao
                    .getLatest()
                    .map { it.instance }
                    .filter {
                        it.startTimestamp > timeProvider.now()
                    }

                if(futureInstances == lastFutureInstances) {
                    throw IllegalStateException("Stuck in infinite loop while cleaning future instances")
                }

                instanceDao.deleteFutureInstances(
                    futureInstances.map { it.id }
                )

                lastFutureInstances = futureInstances
            }
        }
    }
}