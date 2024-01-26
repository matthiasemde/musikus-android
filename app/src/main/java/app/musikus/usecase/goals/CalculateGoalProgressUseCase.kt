package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalType
import app.musikus.usecase.sessions.GetSessionsInTimeframeUseCase
import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CalculateGoalProgressUseCase(
    private val getSessionsInTimeframe: GetSessionsInTimeframeUseCase,
    private val timeProvider: TimeProvider
) {

    private fun progressForGoalInstance(
        description: GoalDescription,
        instance: GoalInstance,
        libraryItemIds: List<UUID>
    ) : Flow<Duration> {

        return getSessionsInTimeframe(
            timeframe = Pair(
                instance.startTimestamp,
                instance.endTimestamp ?: description.endOfInstanceInLocalTimezone(
                    instance = instance,
                    timeProvider = timeProvider
                )
            )
        ).map { sessions ->

            val sections = sessions.flatMap { it.sections }

            if (description.type == GoalType.ITEM_SPECIFIC) {
                sections.sumOf { (section, libraryItem) ->
                    if (libraryItem.id in libraryItemIds) section.duration.inWholeSeconds else 0
                }
            } else {
                sections.sumOf { (section, _) -> section.duration.inWholeSeconds }
            }.seconds
        }
    }

    @JvmName("calculateProgressForGoalInstanceWithDescription")
    operator fun invoke(
        goals: List<GoalInstanceWithDescriptionWithLibraryItems>
    ): Flow<List<Duration>> {
        return combine(goals.map { goal ->
            val descriptionWithLibraryItems = goal.description
            val description = descriptionWithLibraryItems.description
            val libraryItemIds = descriptionWithLibraryItems.libraryItems.map { it.id }
            val instance = goal.instance

            return@map progressForGoalInstance(
                description = description,
                instance = instance,
                libraryItemIds = libraryItemIds
            )
        }) {
            it.toList()
        }
    }

    @JvmName("calculateProgressForGoalDescriptionWithInstance")

    operator fun invoke(
        goals: List<GoalDescriptionWithInstancesAndLibraryItems>
    ): Flow<List<List<Duration>>> {
        return combine(goals.map { goal ->
            val libraryItemIds = goal.libraryItems.map { it.id }

            return@map combine(goal.instances.map { instance ->
                progressForGoalInstance(
                    description = goal.description,
                    instance = instance,
                    libraryItemIds = libraryItemIds
                )
            }) {
                it.toList()
            }
        }) {
            it.toList()
        }
    }
}
