/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.GoalDescriptionWithLibraryItems
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.domain.GoalDescriptionWithInstancesWithProgressAndLibraryItems
import app.musikus.goals.domain.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class GetNextNAfterInstanceUseCase(
    private val goalRepository: GoalRepository,
    private val calculateProgress: CalculateGoalProgressUseCase
) {

    suspend operator fun invoke(
        goalDescriptionWithLibraryItems: GoalDescriptionWithLibraryItems,
        firstInstanceId: UUID,
        n: Int
    ): Flow<GoalDescriptionWithInstancesWithProgressAndLibraryItems> {
        val instances = mutableListOf<GoalInstance>()

        var previousInstanceId: UUID = firstInstanceId

        while (instances.size < n) {
            val instance = goalRepository
                .getInstanceByPreviousInstanceId(previousInstanceId)
                .first()

            if (instance.descriptionId != goalDescriptionWithLibraryItems.description.id) {
                throw(IllegalArgumentException("Instance does not belong to description"))
            }

            instances.add(instance)

            if (instance.endTimestamp == null) break

            previousInstanceId = instance.id
        }

        return calculateProgress(
            listOf(
                GoalDescriptionWithInstancesAndLibraryItems(
                    description = goalDescriptionWithLibraryItems.description,
                    instances = instances,
                    libraryItems = goalDescriptionWithLibraryItems.libraryItems
                )
            )
        ).map { it.single() }.map { progress ->
            GoalDescriptionWithInstancesWithProgressAndLibraryItems(
                description = goalDescriptionWithLibraryItems.description,
                instancesWithProgress = instances.zip(progress),
                libraryItems = goalDescriptionWithLibraryItems.libraryItems
            )
        }
    }
}
