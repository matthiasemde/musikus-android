package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalProgressType
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.InvalidGoalDescriptionException
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
        addGoal = AddGoalUseCase(fakeGoalRepository, fakeTimeProvider)

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
}