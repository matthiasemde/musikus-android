package app.musikus.goals.domain.usecase

import app.musikus.goals.data.GoalRepository
import app.musikus.goals.domain.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class GetLastFiveCompletedGoalsUseCase(
    private val goalRepository: GoalRepository,
    private val calculateProgress: CalculateGoalProgressUseCase
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() : Flow<List<GoalInstanceWithProgressAndDescriptionWithLibraryItems>> {
        return goalRepository.lastFiveCompletedGoals.flatMapLatest { goals ->
            calculateProgress(goals).map { progress ->
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
}