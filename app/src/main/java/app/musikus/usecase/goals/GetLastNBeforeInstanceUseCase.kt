package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalDescriptionWithLibraryItems
import app.musikus.database.daos.GoalInstance
import app.musikus.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class GetLastNBeforeInstanceUseCase(
    private val goalRepository: GoalRepository,
    private val calculateProgress: CalculateGoalProgressUseCase
) {

    suspend operator fun invoke(
        goalDescriptionWithLibraryItems: GoalDescriptionWithLibraryItems,
        lastInstanceId: UUID,
        n: Int
    ) : Flow<GoalDescriptionWithInstancesWithProgressAndLibraryItems> {

        val instances = mutableListOf<GoalInstance>()
        var previousInstanceId: UUID? = lastInstanceId

        while (instances.size < n && previousInstanceId != null) {
            val instance = goalRepository.getInstance(previousInstanceId).first()

            if (instance.descriptionId != goalDescriptionWithLibraryItems.description.id) {
                throw(IllegalArgumentException("Instance does not belong to description"))
            }

            // add to front of list so it is in chronological order
            instances.add(0, instance)
            previousInstanceId = instance.previousInstanceId
        }

        return calculateProgress(
            listOf(GoalDescriptionWithInstancesAndLibraryItems(
                description = goalDescriptionWithLibraryItems.description,
                instances = instances,
                libraryItems = goalDescriptionWithLibraryItems.libraryItems
            ))
        ).map { it.single() }.map { progress ->
            GoalDescriptionWithInstancesWithProgressAndLibraryItems(
                description = goalDescriptionWithLibraryItems.description,
                instancesWithProgress = instances.zip(progress),
                libraryItems = goalDescriptionWithLibraryItems.libraryItems
            )
        }
    }
}