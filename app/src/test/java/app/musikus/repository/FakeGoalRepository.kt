package app.musikus.repository

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalInstanceUpdateAttributes
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

    override suspend fun getLatestInstances(): List<GoalInstanceWithDescription> {
        return _goalDescriptionWithInstancesAndLibraryItems.mapNotNull { goal ->
            goal.instances
                .singleOrNull { instance -> instance.endTimestamp == null }
                ?.let { instance ->
                    GoalInstanceWithDescription(
                        instance = instance,
                        description = goal.description
                    )
                }
        }
    }

    override suspend fun addNewGoal(
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

    override suspend fun addNewInstance(instanceCreationAttributes: GoalInstanceCreationAttributes) {
        val oldGoal = _goalDescriptionWithInstancesAndLibraryItems.single {
            it.description.id == instanceCreationAttributes.goalDescriptionId
        }

        _goalDescriptionWithInstancesAndLibraryItems.remove(oldGoal)
        _goalDescriptionWithInstancesAndLibraryItems.add(
            oldGoal.copy(
                instances = oldGoal.instances + GoalInstance(
                    id = idProvider.generateId(),
                    createdAt = timeProvider.now(),
                    modifiedAt = timeProvider.now(),
                    goalDescriptionId = instanceCreationAttributes.goalDescriptionId,
                    startTimestamp = instanceCreationAttributes.startTimestamp,
                    targetSeconds = instanceCreationAttributes.target.inWholeSeconds,
                    endTimestamp = null
                )
            )
        )
    }

    override suspend fun updateGoalInstance(
        id: UUID,
        updateAttributes: GoalInstanceUpdateAttributes
    ) {
        val oldGoal = _goalDescriptionWithInstancesAndLibraryItems.single {
            it.instances.any { instance -> instance.id == id }
        }

        _goalDescriptionWithInstancesAndLibraryItems.remove(oldGoal)
        _goalDescriptionWithInstancesAndLibraryItems.add(
            oldGoal.copy(
                instances = oldGoal.instances.map { instance ->
                    if (instance.id == id) {
                        instance.copy(
                            modifiedAt = timeProvider.now(),
                            endTimestamp = updateAttributes.endTimestamp?.value
                        )
                    } else {
                        instance
                    }
                }
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

    override suspend fun delete(goals: List<GoalDescription>) {
        TODO("Not yet implemented")
    }

    override suspend fun restore(goals: List<GoalDescription>) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFutureInstances(instanceIds: List<UUID>) {
        _goalDescriptionWithInstancesAndLibraryItems.replaceAll { goal ->
            goal.copy(
                instances = goal.instances.filter { it.id !in instanceIds }
            )
        }
    }

    override suspend fun deletePausedInstance(instanceId: UUID) {
        val oldGoal = _goalDescriptionWithInstancesAndLibraryItems.single {
            it.instances.any { instance -> instance.id == instanceId }
        }

        _goalDescriptionWithInstancesAndLibraryItems.remove(oldGoal)
        _goalDescriptionWithInstancesAndLibraryItems.add(
            oldGoal.copy(
                instances = oldGoal.instances.filter { it.id != instanceId }
            )
        )
    }

    override suspend fun clean() {
        TODO("Not yet implemented")
    }

    override suspend fun withTransaction(block: suspend () -> Unit) {
       block()
    }
}