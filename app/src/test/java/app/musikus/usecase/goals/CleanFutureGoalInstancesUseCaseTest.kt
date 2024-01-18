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
import app.musikus.repository.FakeGoalRepository
import app.musikus.repository.FakeLibraryRepository
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class CleanFutureGoalInstancesUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var addGoalUseCase: AddGoalUseCase
    private lateinit var archiveGoalsUseCase: ArchiveGoalsUseCase
    private lateinit var updateGoalsUseCase: UpdateGoalsUseCase

    /** SUT */
    private lateinit var cleanFutureGoalInstancesUseCase: CleanFutureGoalInstancesUseCase

    private val defaultDescriptionCreationAttributes = GoalDescriptionCreationAttributes(
        type = GoalType.NON_SPECIFIC,
        repeat = true,
        periodInPeriodUnits = 2,
        periodUnit = GoalPeriodUnit.DAY,
    )

    private val defaultInstanceCreationAttributes = GoalInstanceCreationAttributes(
        target = 1.hours,
    )

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        addGoalUseCase = AddGoalUseCase(fakeGoalRepository, fakeLibraryRepository, fakeTimeProvider)
        archiveGoalsUseCase = ArchiveGoalsUseCase(fakeGoalRepository)
        updateGoalsUseCase = UpdateGoalsUseCase(fakeGoalRepository, archiveGoalsUseCase, fakeTimeProvider)

        /** SUT */
        cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(fakeGoalRepository, fakeTimeProvider)

        runBlocking {

        }
    }

    @Test
    fun `clean future goal instances`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = defaultDescriptionCreationAttributes,
            instanceCreationAttributes = defaultInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.advanceTimeBy(6.days)

        updateGoalsUseCase()

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(2.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(2.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(4.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(4),
                        createdAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(3),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(4.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(6.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(5),
                        createdAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(4),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(6.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )

        fakeTimeProvider.revertTimeBy(3.days)

        cleanFutureGoalInstancesUseCase()

        val cleanedGoals = fakeGoalRepository.allGoals.first()

        assertThat(cleanedGoals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(2.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = fakeTimeProvider.startTime.plus(6.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(3.days.toJavaDuration()),
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = UUIDConverter.fromInt(2),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(2.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `clean future goal instances for all future instances, doesn't delete first instance`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = defaultDescriptionCreationAttributes,
            instanceCreationAttributes = defaultInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.revertTimeBy(1.days)

        cleanFutureGoalInstancesUseCase()

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime,
                    type = GoalType.NON_SPECIFIC,
                    repeat = true,
                    periodInPeriodUnits = 2,
                    periodUnit = GoalPeriodUnit.DAY,
                    progressType = GoalProgressType.TIME,
                    paused = false,
                    archived = false,
                    customOrder = null
                ),
                instances = listOf(
                    GoalInstance(
                        id = UUIDConverter.fromInt(2),
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                        descriptionId = UUIDConverter.fromInt(1),
                        previousInstanceId = null,
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }
}