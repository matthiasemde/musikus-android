package app.musikus.usecase.goals

import app.musikus.database.UUIDConverter
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.InvalidGoalDescriptionException
import app.musikus.database.entities.InvalidGoalInstanceException
import app.musikus.repository.GoalRepository
import app.musikus.repository.LibraryRepository
import app.musikus.utils.TimeProvider
import java.util.UUID

class AddGoalUseCase(
    private val goalRepository: GoalRepository,
    private val libraryRepository: LibraryRepository,
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
            throw InvalidGoalInstanceException("Start timestamp must be in the past, it is set automatically")
        }

        // check if the goal description id was changed from the default value
        if(instanceCreationAttributes.descriptionId != UUIDConverter.deadBeef) {
            throw InvalidGoalInstanceException("Goal description id must not be set, it is set automatically")
        }

        if(descriptionCreationAttributes.type == GoalType.NON_SPECIFIC && !libraryItemIds.isNullOrEmpty()) {
            throw InvalidGoalDescriptionException("Library items must be null or empty for non-specific goals")
        }

        if(descriptionCreationAttributes.type == GoalType.ITEM_SPECIFIC) {
            if(libraryItemIds.isNullOrEmpty()) {
                throw InvalidGoalDescriptionException("Item specific goals must have at least one library item")
            }

            val nonExistentLibraryItemIds = libraryItemIds.filter { !libraryRepository.existsItem(it) }
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