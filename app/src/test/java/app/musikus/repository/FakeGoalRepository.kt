package app.musikus.repository

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.utils.IdProvider
import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import kotlin.time.Duration

class FakeGoalRepository(
    private val fakeLibraryRepository: FakeLibraryRepository,
    private val timeProvider: TimeProvider,
    private val idProvider: IdProvider
) : GoalRepository {

    private val _goalDescriptionWithInstancesAndLibraryItems = mutableListOf<GoalDescriptionWithInstancesAndLibraryItems>()
    override val currentGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
        get() = TODO("Not yet implemented")
    override val allGoals: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
        get() = flowOf(_goalDescriptionWithInstancesAndLibraryItems)
    override val lastFiveCompletedGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
        get() = TODO("Not yet implemented")

    override suspend fun add(
        descriptionCreationAttributes: GoalDescriptionCreationAttributes,
        instanceCreationAttributes: GoalInstanceCreationAttributes,
        libraryItemIds: List<UUID>?
    ) {
        val descriptionId = idProvider.generateId()
        val instanceId = idProvider.generateId()

        _goalDescriptionWithInstancesAndLibraryItems.add(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = descriptionId,
                    createdAt = timeProvider.now(),
                    modifiedAt = timeProvider.now(),
                    type = descriptionCreationAttributes.type,
                    repeat = descriptionCreationAttributes.repeat,
                    periodInPeriodUnits = descriptionCreationAttributes.periodInPeriodUnits,
                    periodUnit = descriptionCreationAttributes.periodUnit,
                    progressType = descriptionCreationAttributes.progressType,
                    paused = false,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = instanceId,
                        createdAt = timeProvider.now(),
                        modifiedAt = timeProvider.now(),
                        goalDescriptionId = descriptionId,
                        startTimestamp = instanceCreationAttributes.startTimestamp,
                        targetSeconds = instanceCreationAttributes.target.inWholeSeconds,
                        endTimestamp = null
                    )
                ),
                libraryItems = libraryItemIds?.let { itemIds ->
                    fakeLibraryRepository.items.first().filter { item -> itemIds.contains(item.id) }
                } ?: emptyList()
            )
        )
    }

    override suspend fun updateGoalDescriptions(
        idsWithUpdateAttributes: List<Pair<UUID, GoalDescriptionUpdateAttributes>>
    ) {
        for ((id, updateAttributes) in idsWithUpdateAttributes) {
            val oldGoal = _goalDescriptionWithInstancesAndLibraryItems.first {
                it.description.id == id
            }

            _goalDescriptionWithInstancesAndLibraryItems.remove(oldGoal)
            _goalDescriptionWithInstancesAndLibraryItems.add(
                oldGoal.copy(
                    description = oldGoal.description.copy(
                        modifiedAt = timeProvider.now(),
                        paused = updateAttributes.paused ?: oldGoal.description.paused,
                        archived = updateAttributes.archived ?: oldGoal.description.archived,
                        customOrder = updateAttributes.customOrder?.value ?: oldGoal.description.customOrder
                    )
                )
            )
        }
    }

    override suspend fun editGoalTarget(goal: GoalInstance, newTarget: Duration) {
        TODO("Not yet implemented")
    }


    override suspend fun archive(goal: GoalDescription) {
        TODO("Not yet implemented")
    }

    override suspend fun archive(goals: List<GoalDescription>) {
        TODO("Not yet implemented")
    }

    override suspend fun unarchive(goal: GoalDescription) {
        TODO("Not yet implemented")
    }

    override suspend fun unarchive(goals: List<GoalDescription>) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(goal: GoalDescription) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(goals: List<GoalDescription>) {
        TODO("Not yet implemented")
    }

    override suspend fun restore(goal: GoalDescription) {
        TODO("Not yet implemented")
    }

    override suspend fun restore(goals: List<GoalDescription>) {
        TODO("Not yet implemented")
    }

    override suspend fun clean() {
        TODO("Not yet implemented")
    }

    override suspend fun updateGoals() {
        TODO("Not yet implemented")
    }
}