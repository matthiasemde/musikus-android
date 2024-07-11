package app.musikus.repository

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.GoalDescriptionWithLibraryItems
import app.musikus.core.data.GoalInstanceWithDescription
import app.musikus.core.data.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.core.data.Nullable
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalDescriptionUpdateAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceUpdateAttributes
import app.musikus.goals.data.GoalRepository
import app.musikus.utils.IdProvider
import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeGoalRepository(
    private val fakeLibraryRepository: FakeLibraryRepository,
    private val timeProvider: TimeProvider,
    private val idProvider: IdProvider
) : GoalRepository {

    private val _goalDescriptionWithInstancesAndLibraryItems = mutableListOf<GoalDescriptionWithInstancesAndLibraryItems>()

    private var _goalBuffer = listOf<GoalDescriptionWithInstancesAndLibraryItems>()

    override val currentGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
        get() = flowOf(_goalDescriptionWithInstancesAndLibraryItems.map {
            GoalInstanceWithDescriptionWithLibraryItems(
                instance = it.latestInstance,
                description = GoalDescriptionWithLibraryItems(
                    description = it.description,
                    libraryItems = it.libraryItems
                )
            )
        })
    override val allGoals: Flow<List<GoalDescriptionWithInstancesAndLibraryItems>>
        get() = flowOf(_goalDescriptionWithInstancesAndLibraryItems)
    override val lastFiveCompletedGoals: Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
        get() = throw NotImplementedError()

    override fun getInstance(id: UUID): Flow<GoalInstance> {
        throw NotImplementedError()
    }

    override fun getInstanceByPreviousInstanceId(previousInstanceId: UUID): Flow<GoalInstance> {
        throw NotImplementedError()
    }

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
                        descriptionId = descriptionId,
                        previousInstanceId = instanceCreationAttributes.previousInstanceId,
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
            it.description.id == instanceCreationAttributes.descriptionId
        }

        _goalDescriptionWithInstancesAndLibraryItems.remove(oldGoal)
        _goalDescriptionWithInstancesAndLibraryItems.add(
            oldGoal.copy(
                instances = oldGoal.instances + GoalInstance(
                    id = idProvider.generateId(),
                    createdAt = timeProvider.now(),
                    modifiedAt = timeProvider.now(),
                    descriptionId = instanceCreationAttributes.descriptionId,
                    previousInstanceId = instanceCreationAttributes.previousInstanceId,
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
                            targetSeconds = updateAttributes.target?.inWholeSeconds ?: instance.targetSeconds,
                            endTimestamp = (updateAttributes.endTimestamp ?: Nullable(instance.endTimestamp)).value
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

    override suspend fun delete(descriptionIds: List<UUID>) {
        _goalBuffer = _goalDescriptionWithInstancesAndLibraryItems.filter { goal ->
            goal.description.id in descriptionIds
        }.toMutableList()
        _goalDescriptionWithInstancesAndLibraryItems.removeIf { goal ->
            goal.description.id in descriptionIds
        }
    }

    override suspend fun restore(descriptionIds: List<UUID>) {
        _goalDescriptionWithInstancesAndLibraryItems.addAll(
            _goalBuffer.filter { goal ->
                goal.description.id in descriptionIds
            }
        )
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

    override suspend fun existsDescription(descriptionId: UUID): Boolean {
        return _goalDescriptionWithInstancesAndLibraryItems.any { goal ->
            goal.description.id == descriptionId
        }
    }

    override suspend fun clean() {
        throw NotImplementedError()
    }

    override suspend fun withTransaction(block: suspend () -> Unit) {
       block()
    }
}