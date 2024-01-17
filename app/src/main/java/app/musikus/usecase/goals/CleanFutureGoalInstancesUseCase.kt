package app.musikus.usecase.goals

import app.musikus.database.daos.GoalInstance
import app.musikus.repository.GoalRepository
import app.musikus.utils.TimeProvider

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
                        it.startTimestamp > timeProvider.now()
                    }

                if(futureInstances == lastFutureInstances) {
                    throw IllegalStateException("Stuck in infinite loop while cleaning future instances")
                }

                goalRepository.deleteFutureInstances(
                    futureInstances.map { it.id }
                )

                lastFutureInstances = futureInstances
            }
        }
    }
}