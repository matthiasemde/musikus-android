package app.musikus.usecase.goals

import app.musikus.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetLastFiveCompletedGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val calculateProgress: CalculateGoalProgressUseCase
) {

    operator fun invoke() : Flow<List<GoalInstanceWithProgressAndDescriptionWithLibraryItems>> {
        return goalRepository.lastFiveCompletedGoals.map { goals ->
            val progress = calculateProgress(goals)
            goals.zip(progress).map { (goal, progress) ->
                GoalInstanceWithProgressAndDescriptionWithLibraryItems(
                    description = goal.description,
                    instance = goal.instance,
                    progress = progress,
                )
            }
        }
    }
}