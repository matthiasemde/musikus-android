package app.musikus.goals.domain.usecase

import app.musikus.core.data.GoalDescriptionWithInstancesAndLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalDescriptionUpdateAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalProgressType
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.data.FakeGoalRepository
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class ArchiveGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var cleanFutureGoalInstancesUseCase: CleanFutureGoalInstancesUseCase
    private lateinit var updateGoalsUseCase: UpdateGoalsUseCase

    /** SUT */
    private lateinit var archiveGoalsUseCase: ArchiveGoalsUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(
            goalRepository = fakeGoalRepository,
            timeProvider = fakeTimeProvider,
        )

        /** SUT */
        archiveGoalsUseCase = ArchiveGoalsUseCase(
            goalRepository = fakeGoalRepository,
            cleanFutureGoalInstances = cleanFutureGoalInstancesUseCase,
        )

        updateGoalsUseCase = UpdateGoalsUseCase(
            goalRepository = fakeGoalRepository,
            archiveGoals = archiveGoalsUseCase,
            timeProvider = fakeTimeProvider,
        )

        runBlocking {
            fakeGoalRepository.addNewGoal(
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
    fun `Archive non-existent goal description, IllegalArgumentException`() = runTest {
        val exception = assertThrows<IllegalArgumentException> {
            archiveGoalsUseCase(listOf(UUIDConverter.fromInt(0)))
        }

        assertThat(exception.message).isEqualTo(
            "Could not find goal(s) with descriptionId(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun `Archive already archived, IllegalArgumentException`() = runTest {
        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val exception = assertThrows<IllegalArgumentException> {
            archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))
        }

        assertThat(exception.message).isEqualTo(
            "Cannot archive already archived goal(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun `Archive goal with future instances, future instances are cleaned`() = runTest {
        fakeTimeProvider.advanceTimeBy(1.days)

        updateGoalsUseCase()

        var goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
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
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    ),
                ),
                libraryItems = emptyList()
            )
        )

        fakeTimeProvider.revertTimeBy(1.days)

        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = true,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Archive paused goal, latest instance is deleted`() = runTest {
        fakeTimeProvider.advanceTimeBy(1.days)

        updateGoalsUseCase()

        fakeGoalRepository.updateGoalDescriptions(listOf(
            UUIDConverter.fromInt(1) to GoalDescriptionUpdateAttributes(paused = true)
        ))

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = true,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )

        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val archivedGoals = fakeGoalRepository.allGoals.first()

        assertThat(archivedGoals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = true,
                    archived = true,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = FakeTimeProvider.START_TIME.plus(1.days.toJavaDuration())
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Archive paused goal on first instance, instance is kept`() = runTest {
        fakeGoalRepository.updateGoalDescriptions(listOf(
            UUIDConverter.fromInt(1) to GoalDescriptionUpdateAttributes(paused = true)
        ))

        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = true,
                    archived = true,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `Archive normal goal, goal is archived`() = runTest {
        archiveGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = true,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = FakeTimeProvider.START_TIME,
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }
}