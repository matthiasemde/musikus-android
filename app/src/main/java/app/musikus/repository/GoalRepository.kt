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
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface GoalRepository {
    val currentGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
    val allGoals: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
    val lastFiveCompletedGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    suspend fun getLatestInstances(): List<GoalInstanceWithDescription>

    /** Mutators */
    /** Add */
    suspend fun addNewGoal(
        descriptionCreationAttributes: GoalDescriptionCreationAttributes,
        instanceCreationAttributes: GoalInstanceCreationAttributes,
        libraryItemIds: List<UUID>?,
    )

    suspend fun addNewInstance(
        instanceCreationAttributes: GoalInstanceCreationAttributes
    )

    /** Edit */

    suspend fun updateGoalInstance(
        id: UUID,
        updateAttributes: GoalInstanceUpdateAttributes,
    )

    suspend fun updateGoalDescriptions(
        idsWithUpdateAttributes: List<Pair<UUID, GoalDescriptionUpdateAttributes>>,
    )

    /** Delete / Restore */
    suspend fun delete(descriptionIds: List<UUID>)

    suspend fun restore(descriptionIds: List<UUID>)

    suspend fun deleteFutureInstances(instanceIds: List<UUID>)

    suspend fun deletePausedInstance(instanceId: UUID)

    /** Exists */
    suspend fun existsDescription(descriptionId: UUID): Boolean

    /** Clean */
    suspend fun clean()

    /** Transaction */
    suspend fun withTransaction(block: suspend () -> Unit)

}

class GoalRepositoryImpl(
    private val database: MusikusDatabase,
) : GoalRepository {

    private val instanceDao = database.goalInstanceDao
    private val descriptionDao = database.goalDescriptionDao

    override val currentGoals = instanceDao.getCurrent()
    override val allGoals = descriptionDao.getAllWithInstancesAndLibraryItems()
    override val lastFiveCompletedGoals = instanceDao.getLastNCompleted(5)

    override suspend fun getLatestInstances(): List<GoalInstanceWithDescription> {
        return instanceDao.getLatest()
    }


    /** Mutators */

    /** Add */
    override suspend fun addNewGoal(
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

    override suspend fun addNewInstance(
        instanceCreationAttributes: GoalInstanceCreationAttributes
    ) {
        instanceDao.insert(instanceCreationAttributes)
    }


    /** Edit */

    override suspend fun updateGoalInstance(
        id: UUID,
        updateAttributes: GoalInstanceUpdateAttributes,
    ) {
        instanceDao.update(id, updateAttributes)
    }

    override suspend fun updateGoalDescriptions(
        idsWithUpdateAttributes: List<Pair<UUID, GoalDescriptionUpdateAttributes>>
    ) {
        descriptionDao.update(idsWithUpdateAttributes)
    }


    /** Delete / Restore */

    override suspend fun delete(descriptionIds: List<UUID>) {
        descriptionDao.delete(descriptionIds)
    }

    override suspend fun restore(descriptionIds: List<UUID>) {
        descriptionDao.restore(descriptionIds)
    }

    override suspend fun deleteFutureInstances(instanceIds: List<UUID>) {
        instanceDao.deleteFutureInstances(instanceIds)
    }

    override suspend fun deletePausedInstance(instanceId: UUID) {
        instanceDao.deletePausedInstance(instanceId)
    }

    /** Exists */

    override suspend fun existsDescription(descriptionId: UUID): Boolean {
        return descriptionDao.exists(descriptionId)
    }

    /** Clean */

    override suspend fun clean() {
        descriptionDao.clean()
    }

    /** Transaction */

    override suspend fun withTransaction(block: suspend () -> Unit) {
        return database.withTransaction(block)
    }
}