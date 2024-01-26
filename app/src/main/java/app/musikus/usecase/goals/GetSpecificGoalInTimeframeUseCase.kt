package app.musikus.usecase.goals

import app.musikus.repository.GoalRepository
import app.musikus.utils.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GetSpecificGoalInTimeframeUseCase(
    private val goalRepository: GoalRepository,
    private val calculateProgress: CalculateGoalProgressUseCase
) {

    operator fun invoke(
        descriptionId: UUID,
        timeframe: Timeframe
    ) : Flow<GoalDescriptionWithInstancesWithProgressAndLibraryItems> {
        return goalRepository.getSpecificGoalInTimeframe(
            descriptionId = descriptionId,
            timeframe = timeframe
        ).map { goal ->
            val progress = calculateProgress(listOf(goal)).single()
            GoalDescriptionWithInstancesWithProgressAndLibraryItems(
                description = goal.description,
                instances = goal.instances.zip(progress).map { (instance, progress) ->
                    GoalInstanceWithProgress(
                        instance = instance,
                        progress = progress
                    )
                },
                libraryItems = goal.libraryItems
            )
        }
    }
}