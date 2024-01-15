package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalProgressType
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.InvalidGoalDescriptionException
import app.musikus.database.entities.InvalidGoalInstanceException
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.repository.FakeGoalRepository
import app.musikus.repository.FakeLibraryRepository
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class AddGoalUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository

    private lateinit var addGoal: AddGoalUseCase
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var validDescriptionCreationAttributes: GoalDescriptionCreationAttributes
    private lateinit var validInstanceCreationAttributes: GoalInstanceCreationAttributes


    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()
        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)
        addGoal = AddGoalUseCase(fakeGoalRepository, fakeLibraryRepository, fakeTimeProvider)

        validDescriptionCreationAttributes = GoalDescriptionCreationAttributes(
            type = GoalType.NON_SPECIFIC,
            repeat = true,
            periodInPeriodUnits = 1,
            periodUnit = GoalPeriodUnit.DAY,
        )

        validInstanceCreationAttributes = GoalInstanceCreationAttributes(
            target = 1.hours,
            startTimestamp = fakeTimeProvider.now(),
        )
    }

    @Test
    fun `Add valid non-specific goal, goalRepository contains goal`() = runTest {
        addGoal(
            descriptionCreationAttributes = validDescriptionCreationAttributes,
            instanceCreationAttributes = validInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = fakeTimeProvider.now(),
                    modifiedAt = fakeTimeProvider.now(),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = fakeTimeProvider.now(),
                        modifiedAt = fakeTimeProvider.now(),
                        goalDescriptionId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.now(),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Add valid item-specific goal, goalRepository contains goal`() = runTest {
        fakeLibraryRepository.addItem(
            LibraryItemCreationAttributes(
                name = "Item 1",
                colorIndex = 0,
                libraryFolderId = Nullable(null)
            )
        )

        addGoal(
            descriptionCreationAttributes = validDescriptionCreationAttributes.copy(
                type = GoalType.ITEM_SPECIFIC,
            ),
            instanceCreationAttributes = validInstanceCreationAttributes,
            libraryItemIds = listOf(UUIDConverter.fromInt(1))
        )

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(2),
                    createdAt = fakeTimeProvider.now(),
                    modifiedAt = fakeTimeProvider.now(),
                    type = GoalType.ITEM_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = fakeTimeProvider.now(),
                        modifiedAt = fakeTimeProvider.now(),
                        goalDescriptionId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.now(),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = listOf(
                    LibraryItem(
                        id = UUIDConverter.fromInt(1),
                        createdAt = fakeTimeProvider.now(),
                        modifiedAt = fakeTimeProvider.now(),
                        name = "Item 1",
                        colorIndex = 0,
                        customOrder = null,
                        libraryFolderId = null
                    )
                )
            )
        )
    }

    @Test
    fun `Add goal with period 0, InvalidGoalDescriptionException('Period in period units must be greater than 0')`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes.copy(
                    periodInPeriodUnits = 0
                ),
                instanceCreationAttributes = validInstanceCreationAttributes,
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Period in period units must be greater than 0")
    }

    @Test
    fun `Add goal with target 0, InvalidGoalDescriptionException('Target must be finite and greater than 0')`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes.copy(
                    target = 0.hours
                ),
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Target must be finite and greater than 0")
    }

    @Test
    fun `Add goal with infinite target, InvalidGoalDescriptionException('Target must be finite and greater than 0')`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes.copy(
                    target = Double.POSITIVE_INFINITY.hours
                ),
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Target must be finite and greater than 0")
    }

    @Test
    fun `Add goal with startTimestamp in the future, InvalidGoalInstanceException('Start timestamp must be in the past')`() = runTest {
        val exception = assertThrows<InvalidGoalInstanceException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes.copy(
                    startTimestamp = fakeTimeProvider.now().plus(1.hours.toJavaDuration())
                ),
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Start timestamp must be in the past")
    }

    @Test
    fun `Add goal with goalDescriptionId set, InvalidGoalInstanceException('Goal description id must not be set, it is set automatically')`() = runTest {
        val exception = assertThrows<InvalidGoalInstanceException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes.copy(
                    goalDescriptionId = UUIDConverter.fromInt(1)
                ),
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Goal description id must not be set, it is set automatically")
    }

    @Test
    fun `Add non-specific goal with library items, InvalidGoalDescriptionException('Library items must be null or empty for non-specific goals')`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes,
                instanceCreationAttributes = validInstanceCreationAttributes,
                libraryItemIds = listOf(UUIDConverter.fromInt(1))
            )
        }

        assertThat(exception.message).isEqualTo("Library items must be null or empty for non-specific goals")
    }

    @Test
    fun `Add item-specific goal without library items, InvalidGoalDescriptionException('Item specific goals must have at least one library item')`() = runTest {
        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes.copy(
                    type = GoalType.ITEM_SPECIFIC
                ),
                instanceCreationAttributes = validInstanceCreationAttributes,
                libraryItemIds = emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Item specific goals must have at least one library item")
    }

    @Test
    fun `Add item-specific goal with non-existent library items, InvalidGoalDescriptionException('Library items do not exist')`() = runTest {
        fakeLibraryRepository.addItem(
            LibraryItemCreationAttributes(
                name = "Item 1",
                colorIndex = 0,
                libraryFolderId = Nullable(null)
            )
        )

        val exception = assertThrows<InvalidGoalDescriptionException> {
            addGoal(
                descriptionCreationAttributes = validDescriptionCreationAttributes.copy(
                    type = GoalType.ITEM_SPECIFIC
                ),
                instanceCreationAttributes = validInstanceCreationAttributes,
                libraryItemIds = listOf(
                    UUIDConverter.fromInt(1),
                    UUIDConverter.fromInt(2)
                )
            )
        }

        assertThat(exception.message).isEqualTo("Library items do not exist: [00000000-0000-0000-0000-000000000002]")
    }
}