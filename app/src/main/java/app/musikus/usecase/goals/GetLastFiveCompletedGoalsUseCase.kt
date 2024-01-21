package app.musikus.usecase.goals

import app.musikus.repository.GoalRepository

class GetLastFiveCompletedGoalsUseCase(
    private val goalRepository: GoalRepository
) {

    operator fun invoke() = goalRepository.lastFiveCompletedGoals
}