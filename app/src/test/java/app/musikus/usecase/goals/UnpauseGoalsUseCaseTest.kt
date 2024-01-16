package app.musikus.usecase.goals

import app.musikus.database.UUIDConverter
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.repository.FakeGoalRepository
import app.musikus.repository.FakeLibraryRepository
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

class UnpauseGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var unpauseGoals: UnpauseGoalsUseCase

    private lateinit var pauseGoals: PauseGoalsUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        val cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(fakeGoalRepository, fakeTimeProvider)

        pauseGoals = PauseGoalsUseCase(fakeGoalRepository, cleanFutureGoalInstancesUseCase)

        unpauseGoals = UnpauseGoalsUseCase(fakeGoalRepository)

        runBlocking {
            fakeGoalRepository.add(
                descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                ),
                instanceCreationAttributes = GoalInstanceCreationAttributes(
                    target = 1.hours,
                    startTimestamp = fakeTimeProvider.now(),
                ),
                libraryItemIds = emptyList()
            )
        }
    }

    @Test
    fun `Unpause paused goal, goal is unpaused`() = runTest {
        pauseGoals(listOf(UUIDConverter.fromInt(1)))

        val pausedGoal = fakeGoalRepository.allGoals.first().first()
        Truth.assertThat(pausedGoal.description.paused).isTrue()

        unpauseGoals(listOf(UUIDConverter.fromInt(1)))

        val unpausedGoal = fakeGoalRepository.allGoals.first().first()
        Truth.assertThat(unpausedGoal.description.paused).isFalse()
    }

    @Test
    fun `Unpause non existent goal, InvalidArgumentException`() = runTest {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            unpauseGoals(listOf(UUIDConverter.fromInt(2)))
        }

        Truth.assertThat(exception.message).isEqualTo(
            "Could not find goal(s) with descriptionId: [00000000-0000-0000-0000-000000000002]"
        )
    }

    @Test
    fun `Unpause archived goal, InvalidArgumentException`() = runTest {
        fakeGoalRepository.updateGoalDescriptions(listOf(
            UUIDConverter.fromInt(1) to GoalDescriptionUpdateAttributes(archived = true)
        ))

        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            unpauseGoals(listOf(UUIDConverter.fromInt(1)))
        }

        Truth.assertThat(exception.message).isEqualTo(
            "Cannot unpause archived goals: [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun `Unpause non-paused goal, InvalidArgumentException`() = runTest {
        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            unpauseGoals(listOf(UUIDConverter.fromInt(1)))
        }

        Truth.assertThat(exception.message).isEqualTo(
            "Cannot unpause goals that aren't paused: [00000000-0000-0000-0000-000000000001]"
        )
    }
}