/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.Nullable
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalInstanceUpdateAttributes
import app.musikus.goals.data.GoalRepository
import app.musikus.core.domain.TimeProvider

class CleanFutureGoalInstancesUseCase(
    private val goalRepository: GoalRepository,
    private val timeProvider: TimeProvider,
) {

    @Throws(IllegalStateException::class)
    suspend operator fun invoke() {
        var lastFutureInstances : List<GoalInstance>? = null

        while(lastFutureInstances == null || lastFutureInstances.isNotEmpty()) {
            goalRepository.withTransaction {
                val futureInstances = goalRepository
                    .getLatestInstances()
                    .map { it.instance }
                    .filter {
                        it.startTimestamp > timeProvider.now() &&
                        it.previousInstanceId != null // filter out the first instances of each goal
                    }

                if(futureInstances == lastFutureInstances) {
                    throw IllegalStateException("Stuck in infinite loop while cleaning future instances")
                }

                for (futureInstance in futureInstances) {
                    if(futureInstance.previousInstanceId == null) {
                        throw IllegalStateException("Illegally tried to clean first instance of goal")
                    }

                    goalRepository.deleteFutureInstances(listOf(futureInstance.id))
                    goalRepository.updateGoalInstance(
                        futureInstance.previousInstanceId,
                        GoalInstanceUpdateAttributes(
                            endTimestamp = Nullable(null),
                        )
                    )
                }

                lastFutureInstances = futureInstances
            }
        }
    }
}