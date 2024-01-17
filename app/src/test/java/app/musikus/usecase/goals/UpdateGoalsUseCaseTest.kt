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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

class UpdateGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    private lateinit var archiveGoalsUseCase: ArchiveGoalsUseCase
    private lateinit var addGoalUseCase: AddGoalUseCase
    private lateinit var pauseGoalsUseCase: PauseGoalsUseCase
    private lateinit var cleanFutureGoalInstancesUseCase: CleanFutureGoalInstancesUseCase

    /** SUT */
    private lateinit var updateGoalsUseCase: UpdateGoalsUseCase

    private val goalDescriptionCreationAttributes = GoalDescriptionCreationAttributes(
        type = GoalType.NON_SPECIFIC,
        repeat = true,
        periodInPeriodUnits = 1,
        periodUnit = GoalPeriodUnit.DAY,
    )

    private val goalInstanceCreationAttributes = GoalInstanceCreationAttributes(
        target = 1.hours,
    )

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(
            fakeLibraryRepository,
            fakeTimeProvider,
            fakeIdProvider
        )

        addGoalUseCase = AddGoalUseCase(
            fakeGoalRepository,
            fakeLibraryRepository,
            fakeTimeProvider
        )
        archiveGoalsUseCase = ArchiveGoalsUseCase(fakeGoalRepository)
        cleanFutureGoalInstancesUseCase = CleanFutureGoalInstancesUseCase(
            fakeGoalRepository,
            fakeTimeProvider
        )
        pauseGoalsUseCase = PauseGoalsUseCase(
            fakeGoalRepository,
            cleanFutureGoalInstancesUseCase
        )

        /** SUT */
        updateGoalsUseCase = UpdateGoalsUseCase(
            goalRepository = fakeGoalRepository,
            archiveGoals = archiveGoalsUseCase,
            timeProvider = fakeTimeProvider,
        )
    }

    @Test
    fun `update basic goal`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes,
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.advanceTimeBy(2.days)

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
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                        goalDescriptionId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(1.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                        goalDescriptionId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(1.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(2.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(4),
                        createdAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                        goalDescriptionId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(2.days.toJavaDuration()),
                        targetSeconds = 3600,
                        endTimestamp = null
                    ),
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `update non-repeat goal, goal is archived`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes.copy(
                repeat = false
            ),
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.advanceTimeBy(2.days)

        updateGoalsUseCase()

        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                    type = GoalType.NON_SPECIFIC,
                    repeat = false,
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
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                        goalDescriptionId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(1.days.toJavaDuration())
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }

    @Test
    fun `update paused goal, previous instance is removed`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes,
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.advanceTimeBy(1.days)
        updateGoalsUseCase()

        pauseGoalsUseCase(listOf(UUIDConverter.fromInt(1)))

        fakeTimeProvider.advanceTimeBy(1.days)
        updateGoalsUseCase()


        val goals = fakeGoalRepository.allGoals.first()

        assertThat(goals).containsExactly(
            GoalDescriptionWithInstancesAndLibraryItems(
                description = GoalDescription(
                    id = UUIDConverter.fromInt(1),
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
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
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime.plus(1.days.toJavaDuration()),
                        goalDescriptionId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(1.days.toJavaDuration())
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(4),
                        createdAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                        modifiedAt = fakeTimeProvider.startTime.plus(2.days.toJavaDuration()),
                        goalDescriptionId = UUIDConverter.fromInt(1),
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
    fun `update goal in another time zone`() = runTest {
        addGoalUseCase(
            descriptionCreationAttributes = goalDescriptionCreationAttributes,
            instanceCreationAttributes = goalInstanceCreationAttributes,
            libraryItemIds = emptyList()
        )

        fakeTimeProvider.moveToTimezone(ZoneId.of("Europe/Berlin"))

        fakeTimeProvider.advanceTimeBy(1.days)

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
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime.plus(
                            (1.days).toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin")),
                        goalDescriptionId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ),
                        targetSeconds = 3600,
                        endTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(
                            (1.days - 1.hours).toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin"))
                    ),
                    GoalInstance(
                        id = UUIDConverter.fromInt(3),
                        createdAt = fakeTimeProvider.startTime.plus(
                            (1.days).toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin")),
                        modifiedAt = fakeTimeProvider.startTime.plus(
                            (1.days).toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin")),
                        goalDescriptionId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.getStartOfDay(
                            dateTime = fakeTimeProvider.startTime
                        ).plus(
                            (1.days - 1.hours).toJavaDuration()
                        ).withZoneSameInstant(ZoneId.of("Europe/Berlin")),
                        targetSeconds = 3600,
                        endTimestamp = null
                    )
                ),
                libraryItems = emptyList()
            )
        )
    }
}