package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalDescriptionWithLibraryItems
import app.musikus.database.daos.GoalInstance
import app.musikus.repository.GoalRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class GetLastNBeforeInstanceUseCase(
    private val goalRepository: GoalRepository,
) {

    suspend operator fun invoke(
        goalDescriptionWithLibraryItems: GoalDescriptionWithLibraryItems,
        instanceId: UUID,
        n: Int
    ) : GoalDescriptionWithInstancesAndLibraryItems {
        val instances = mutableListOf<GoalInstance>()
        var previousInstanceId: UUID? = instanceId

        while (instances.size < n && previousInstanceId != null) {
            val instance = goalRepository.getInstance(previousInstanceId).first()

            // add to front of list so it is in chronological order
            instances.add(0, instance)
            previousInstanceId = instance.previousInstanceId
        }

        return GoalDescriptionWithInstancesAndLibraryItems(
            description = goalDescriptionWithLibraryItems.description,
            instances = instances,
            libraryItems = goalDescriptionWithLibraryItems.libraryItems
        )
    }
}