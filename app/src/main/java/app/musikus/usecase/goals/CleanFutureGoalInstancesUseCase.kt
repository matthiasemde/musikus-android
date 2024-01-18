package app.musikus.usecase.goals

import app.musikus.database.Nullable
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalInstanceUpdateAttributes
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
                        it.startTimestamp > timeProvider.now() &&
                        it.previousInstanceId != null // filter out the first instances of each goal
                    }

                if(futureInstances == lastFutureInstances) {
                    throw IllegalStateException("Stuck in infinite loop while cleaning future instances")
                }

                for (futureInstance in futureInstances) {
                    if(futureInstance.previousInstanceId == null) {
                        throw IllegalStateException("Illegally tried to clean first instance of goal")
                    }

                    goalRepository.deleteFutureInstances(listOf(futureInstance.id))
                    goalRepository.updateGoalInstance(
                        futureInstance.previousInstanceId,
                        GoalInstanceUpdateAttributes(
                            endTimestamp = Nullable(null),
                        )
                    )
                }

                lastFutureInstances = futureInstances
            }
        }
    }
}