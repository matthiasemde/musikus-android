package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalType
import app.musikus.usecase.sessions.GetSessionsInTimeframeUseCase
import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.first
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CalculateGoalProgressUseCase(
    private val getSessionsInTimeframe: GetSessionsInTimeframeUseCase,
    private val timeProvider: TimeProvider
) {

    private suspend fun progressForGoalInstance(
        description: GoalDescription,
        instance: GoalInstance,
        libraryItemIds: List<UUID>
    ) : Duration {

        val sessions = getSessionsInTimeframe(
            timeframe = Pair(
                instance.startTimestamp,
                instance.endTimestamp ?: description.endOfInstanceInLocalTimezone(
                    instance = instance,
                    timeProvider = timeProvider
                )
            )
        ).first()

        val sections = sessions.flatMap { it.sections }

        val progress = if (description.type == GoalType.ITEM_SPECIFIC) {
            sections.sumOf { (section, libraryItem) ->
                if (libraryItem.id in libraryItemIds) section.duration.inWholeSeconds else 0
            }
        } else {
            sections.sumOf { (section, _) -> section.duration.inWholeSeconds }
        }.seconds

        return progress
    }

    @JvmName("calculateProgressForGoalInstanceWithDescription")
    suspend operator fun invoke(
        goals: List<GoalInstanceWithDescriptionWithLibraryItems>
    ): List<Duration> {
        return goals.map { goal ->
            val descriptionWithLibraryItems = goal.description
            val description = descriptionWithLibraryItems.description
            val libraryItemIds = descriptionWithLibraryItems.libraryItems.map { it.id }
            val instance = goal.instance

            return@map progressForGoalInstance(
                description = description,
                instance = instance,
                libraryItemIds = libraryItemIds
            )
        }
    }

    @JvmName("calculateProgressForGoalDescriptionWithInstance")

    suspend operator fun invoke(
        goals: List<GoalDescriptionWithInstancesAndLibraryItems>
    ): List<List<Duration>> {
        return goals.map { goal ->
            val libraryItemIds = goal.libraryItems.map { it.id }

            return@map goal.instances.map { instance ->
                progressForGoalInstance(
                    description = goal.description,
                    instance = instance,
                    libraryItemIds = libraryItemIds
                )
            }
        }
    }
}
