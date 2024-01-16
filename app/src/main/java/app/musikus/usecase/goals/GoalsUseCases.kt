package app.musikus.usecase.goals

data class GoalsUseCases(
    val add: AddGoalUseCase,
    val pause: PauseGoalsUseCase,
    val unpause: UnpauseGoalsUseCase
)