/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.GoalDescriptionWithLibraryItems
import app.musikus.core.data.GoalInstanceWithDescription
import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.utils.UiText
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalDescriptionUpdateAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceUpdateAttributes
import app.musikus.library.data.daos.LibraryItem
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.time.Duration

interface GoalRepository {
    val currentGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
    val allGoals: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
    val lastFiveCompletedGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>

    fun getInstance(id: UUID): Flow<GoalInstance>
    fun getInstanceByPreviousInstanceId(previousInstanceId: UUID): Flow<GoalInstance>

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

data class GoalDescriptionWithInstancesWithProgressAndLibraryItems(
    val description: GoalDescription,
    val instancesWithProgress: List<Pair<GoalInstance, Duration>>,
    val libraryItems: List<LibraryItem>
) {
    val goalDescriptionWithLibraryItems by lazy {
        GoalDescriptionWithLibraryItems(
            description = description,
            libraryItems = libraryItems
        )
    }
}

data class GoalInstanceWithProgressAndDescriptionWithLibraryItems(
    val description: GoalDescriptionWithLibraryItems,
    val instance: GoalInstance,
    val progress: Duration
) {
    val title: UiText
        get() = description.title

    val subtitle by lazy { description.subtitle(instance) }

    fun endTimestampInLocalTimezone(timeProvider: TimeProvider) =
        description.endOfInstanceInLocalTimezone(instance, timeProvider)
}
