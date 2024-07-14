/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.goals.domain.usecase

import app.musikus.core.data.UUIDConverter
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.data.entities.InvalidGoalDescriptionException
import app.musikus.goals.data.entities.InvalidGoalInstanceException
import app.musikus.goals.data.GoalRepository
import app.musikus.library.domain.usecase.GetAllLibraryItemsUseCase
import app.musikus.core.domain.TimeProvider
import kotlinx.coroutines.flow.first
import java.util.UUID

class AddGoalUseCase(
    private val goalRepository: GoalRepository,
    private val getLibraryItems: GetAllLibraryItemsUseCase,
    private val timeProvider: TimeProvider
) {

    @Throws(InvalidGoalDescriptionException::class, InvalidGoalInstanceException::class)
    suspend operator fun invoke(
        descriptionCreationAttributes: GoalDescriptionCreationAttributes,
        instanceCreationAttributes: GoalInstanceCreationAttributes,
        libraryItemIds: List<UUID>?
    ) {
        if(descriptionCreationAttributes.periodInPeriodUnits <= 0) {
            throw InvalidGoalDescriptionException("Period in period units must be greater than 0")
        }

        if(!(
            instanceCreationAttributes.target.isFinite() &&
            instanceCreationAttributes.target.inWholeSeconds > 0
        )) {
            throw InvalidGoalInstanceException("Target must be finite and greater than 0")
        }

        if(instanceCreationAttributes.startTimestamp != TimeProvider.uninitializedDateTime) {
            throw InvalidGoalInstanceException("Start timestamp must not be set, it is set automatically")
        }

        // check if the goal description id was changed from the default value
        if(instanceCreationAttributes.descriptionId != UUIDConverter.deadBeef) {
            throw InvalidGoalInstanceException("Goal description id must not be set, it is set automatically")
        }

        if(descriptionCreationAttributes.type == GoalType.NON_SPECIFIC && !libraryItemIds.isNullOrEmpty()) {
            throw InvalidGoalDescriptionException("Library items must be null or empty for non-specific goals, but was $libraryItemIds")
        }

        if(descriptionCreationAttributes.type == GoalType.ITEM_SPECIFIC) {
            if(libraryItemIds.isNullOrEmpty()) {
                throw InvalidGoalDescriptionException("Item specific goals must have at least one library item")
            }

            val allLibraryItemIds = getLibraryItems().first().map { it.id }.toSet()
            val nonExistentLibraryItemIds = libraryItemIds - allLibraryItemIds
            if(nonExistentLibraryItemIds.isNotEmpty()) {
                throw InvalidGoalDescriptionException("Library items do not exist: $nonExistentLibraryItemIds")
            }
        }

        // adjust the startTimestamp to be at the beginning of the current period
        val startTimestamp = when(descriptionCreationAttributes.periodUnit) {
            GoalPeriodUnit.DAY -> timeProvider.getStartOfDay()
            GoalPeriodUnit.WEEK -> timeProvider.getStartOfWeek()
            GoalPeriodUnit.MONTH -> timeProvider.getStartOfMonth()
        }

        goalRepository.addNewGoal(
            descriptionCreationAttributes = descriptionCreationAttributes,
            instanceCreationAttributes = instanceCreationAttributes.copy(
                startTimestamp = startTimestamp
            ),
            libraryItemIds = libraryItemIds
        )
    }
}