package app.musikus.goals.domain.usecase

import app.musikus.core.data.Nullable
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.goals.data.FakeGoalRepository
import app.musikus.goals.data.entities.GoalDescriptionCreationAttributes
import app.musikus.goals.data.entities.GoalInstanceCreationAttributes
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalProgressType
import app.musikus.goals.data.entities.GoalType
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import app.musikus.sessions.domain.usecase.GetSessionsInTimeframeUseCase
import app.musikus.sessions.data.FakeSessionRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class CalculateGoalProgressUseCaseTest {


    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeSessionRepository: FakeSessionRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    /** SUT */
    private lateinit var calculateGoalProgress: CalculateGoalProgressUseCase

    @BeforeEach
    fun setUp() {

        val fakeTimeProvider = FakeTimeProvider()
        val fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeSessionRepository = FakeSessionRepository(
            fakeLibraryRepository,
            fakeTimeProvider,
            fakeIdProvider
        )

        fakeGoalRepository = FakeGoalRepository(
            fakeLibraryRepository,
            fakeTimeProvider,
            fakeIdProvider
        )

        val getSessionsInTimeframe = GetSessionsInTimeframeUseCase(fakeSessionRepository)

        /** SUT */
        calculateGoalProgress = CalculateGoalProgressUseCase(
            getSessionsInTimeframe,
            timeProvider = fakeTimeProvider
        )


        // Set up test data
        runBlocking {
            fakeLibraryRepository.addItem(
                LibraryItemCreationAttributes(
                name = "Test Item 1",
                colorIndex = 5,
                libraryFolderId = Nullable(null)
            )
            )
        }
    }

    @Test
    fun `calculate progress for non-specific goal`() = runTest {
        fakeGoalRepository.addNewGoal(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                descriptionId = UUIDConverter.fromInt(2),
                previousInstanceId = null,
                startTimestamp = FakeTimeProvider.START_TIME,
                target = 1.hours
            ),
            libraryItemIds = null
        )

        // add a session during the goals active period
        fakeSessionRepository.add(
            sessionCreationAttributes = SessionCreationAttributes(
                rating = 3,
                breakDuration = 10.minutes,
                comment = "Test comment"
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        2.days.toJavaDuration() // section should still be counted towards the goal because the session started during the goals runtime
                    ),
                    duration = 1.minutes
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME,
                    duration = 2.minutes
                ),
            )
        )

        // add a session after the goal instance has ended
        fakeSessionRepository.add(
            sessionCreationAttributes = SessionCreationAttributes(
                rating = 3,
                breakDuration = 10.minutes,
                comment = "Test comment"
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        1.days.toJavaDuration()
                    ),
                    duration = 10.minutes
                )
            )
        )

        val goals = fakeGoalRepository.allGoals.first()

        val goalProgress = calculateGoalProgress(goals = goals).first()

        assertThat(goalProgress).isEqualTo(listOf(listOf(3.minutes)))
    }

    @Test
    fun `calculate progress for item-specific goal`() = runTest {
        fakeLibraryRepository.addItem(
            LibraryItemCreationAttributes(
            name = "Test Item 2",
            colorIndex = 5,
            libraryFolderId = Nullable(null)
        )
        )

        fakeGoalRepository.addNewGoal(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.ITEM_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                descriptionId = UUIDConverter.fromInt(2),
                previousInstanceId = null,
                startTimestamp = FakeTimeProvider.START_TIME,
                target = 1.hours
            ),
            libraryItemIds = listOf(
                UUIDConverter.fromInt(1),
            )
        )

        // add a session during the goals active period
        fakeSessionRepository.add(
            sessionCreationAttributes = SessionCreationAttributes(
                rating = 3,
                breakDuration = 10.minutes,
                comment = "Test comment"
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME,
                    duration = 1.minutes
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(2),
                    startTimestamp = FakeTimeProvider.START_TIME,
                    duration = 2.minutes
                ),
            )
        )

        val goals = fakeGoalRepository.allGoals.first()

        val goalProgress = calculateGoalProgress(goals = goals).first()

        assertThat(goalProgress).isEqualTo(listOf(listOf(1.minutes)))
    }
}