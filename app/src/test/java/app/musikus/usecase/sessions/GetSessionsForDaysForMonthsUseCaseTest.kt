package app.musikus.usecase.sessions

import app.musikus.core.data.Nullable
import app.musikus.core.data.SectionWithLibraryItem
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.library.data.daos.LibraryItem
import app.musikus.sessionslist.data.daos.Section
import app.musikus.sessionslist.data.daos.Session
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.sessionslist.data.entities.SectionCreationAttributes
import app.musikus.sessionslist.data.entities.SessionCreationAttributes
import app.musikus.repository.FakeLibraryRepository
import app.musikus.repository.FakeSessionRepository
import app.musikus.sessionslist.domain.SessionsForDay
import app.musikus.sessionslist.domain.SessionsForDaysForMonth
import app.musikus.sessionslist.domain.usecase.GetSessionsForDaysForMonthsUseCase
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class GetSessionsForDaysForMonthsUseCaseTest {

    private lateinit var fakeSessionRepository: FakeSessionRepository

    /** SUT */
    private lateinit var getSessionsForDaysForMonths: GetSessionsForDaysForMonthsUseCase

    @BeforeEach
    fun setUp() {
        val fakeTimeProvider = FakeTimeProvider()
        val fakeIdProvider = FakeIdProvider()

        val fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeSessionRepository = FakeSessionRepository(
            fakeLibraryRepository,
            fakeTimeProvider,
            fakeIdProvider
        )


        /** SUT */

        getSessionsForDaysForMonths = GetSessionsForDaysForMonthsUseCase(
            sessionsRepository = fakeSessionRepository,
        )

        runBlocking {
            fakeLibraryRepository.addItem(
                LibraryItemCreationAttributes(
                name = "Test item 1",
                colorIndex = 5,
                libraryFolderId = Nullable(null)
            )
            )
        }
    }

    @Test
    fun `getSessionsForDaysForMonths after inserting sessions`() = runTest {
        val sessionCreationAttributes = SessionCreationAttributes(
            rating = 3,
            breakDuration = 10.minutes,
            comment = "Test comment"
        )

        // Add the first session
        fakeSessionRepository.add(
            sessionCreationAttributes = sessionCreationAttributes,
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        5.seconds.toJavaDuration()
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

        // add a session in the next month
        fakeSessionRepository.add(
            sessionCreationAttributes = sessionCreationAttributes,
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        35.days.toJavaDuration()
                    ),
                    duration = 4.minutes
                ),
            )
        )

        // add a session on the day after the first session
        fakeSessionRepository.add(
            sessionCreationAttributes = sessionCreationAttributes,
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        1.days.toJavaDuration()
                    ),
                    duration = 5.minutes
                )
            )
        )


        // add another session overlapping the first one
        fakeSessionRepository.add(
            sessionCreationAttributes = sessionCreationAttributes,
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        3.seconds.toJavaDuration()
                    ),
                    duration = 6.minutes
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        8.seconds.toJavaDuration()
                    ),
                    duration = 7.minutes
                ),
            )
        )

        val sessionsForDaysForMonths = getSessionsForDaysForMonths().first()

        val expectedLibraryItem = LibraryItem(
            id = UUIDConverter.fromInt(1),
            createdAt = FakeTimeProvider.START_TIME,
            modifiedAt = FakeTimeProvider.START_TIME,
            name = "Test item 1",
            colorIndex = 5,
            libraryFolderId = null,
            customOrder = null
        )

        val expectedSession : (Int) -> Session = { id ->
            Session(
                id = UUIDConverter.fromInt(id),
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME,
                breakDurationSeconds = 600,
                rating = 3,
                comment = "Test comment"
            )
        }

        assertThat(sessionsForDaysForMonths).isEqualTo(listOf(
            SessionsForDaysForMonth(
                specificMonth = 23_636, // (1969 * 12 + 8) months
                sessionsForDays = listOf(
                    SessionsForDay(
                        specificDay = 720_890, // (1969 * 366 + 236) days
                        totalPracticeDuration = 4.minutes,
                        sessions = listOf(
                            SessionWithSectionsWithLibraryItems(
                                session = expectedSession(5),
                                sections = listOf(
                                    SectionWithLibraryItem(
                                        section = Section(
                                            id = UUIDConverter.fromInt(6),
                                            sessionId = UUIDConverter.fromInt(5),
                                            libraryItemId = UUIDConverter.fromInt(1),
                                            durationSeconds = 240,
                                            startTimestamp = FakeTimeProvider.START_TIME.plus(
                                                35.days.toJavaDuration()
                                            ),
                                        ),
                                        libraryItem = expectedLibraryItem
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            SessionsForDaysForMonth(
                specificMonth = 23_635, // (1969 * 12 + 7) months
                sessionsForDays = listOf(
                    SessionsForDay(
                        specificDay = 720_856, // (1969 * 366 + 202) days
                        totalPracticeDuration = 5.minutes,
                        sessions = listOf(
                            SessionWithSectionsWithLibraryItems(
                                session = expectedSession(7),
                                sections = listOf(
                                    SectionWithLibraryItem(
                                        section = Section(
                                            id = UUIDConverter.fromInt(8),
                                            sessionId = UUIDConverter.fromInt(7),
                                            libraryItemId = UUIDConverter.fromInt(1),
                                            durationSeconds = 300,
                                            startTimestamp = FakeTimeProvider.START_TIME.plus(
                                                1.days.toJavaDuration()
                                            ),
                                        ),
                                        libraryItem = expectedLibraryItem
                                    )
                                )
                            )
                        )
                    ),
                    SessionsForDay(
                        specificDay = 720_855, // (1969 * 366 + 201) days
                        totalPracticeDuration = 16.minutes,
                        sessions = listOf(
                            SessionWithSectionsWithLibraryItems(
                                session = expectedSession(9),
                                sections = listOf(
                                    SectionWithLibraryItem(
                                        section = Section(
                                            id = UUIDConverter.fromInt(10),
                                            sessionId = UUIDConverter.fromInt(9),
                                            libraryItemId = UUIDConverter.fromInt(1),
                                            durationSeconds = 360,
                                            startTimestamp = FakeTimeProvider.START_TIME.plus(
                                                3.seconds.toJavaDuration()
                                            ),
                                        ),
                                        libraryItem = expectedLibraryItem
                                    ),
                                    SectionWithLibraryItem(
                                        section = Section(
                                            id = UUIDConverter.fromInt(11),
                                            sessionId = UUIDConverter.fromInt(9),
                                            libraryItemId = UUIDConverter.fromInt(1),
                                            durationSeconds = 420,
                                            startTimestamp = FakeTimeProvider.START_TIME.plus(
                                                8.seconds.toJavaDuration()
                                            ),
                                        ),
                                        libraryItem = expectedLibraryItem
                                    )
                                )
                            ),
                            SessionWithSectionsWithLibraryItems(
                                session = expectedSession(2),
                                sections = listOf(
                                    SectionWithLibraryItem(
                                        section = Section(
                                            id = UUIDConverter.fromInt(4),
                                            sessionId = UUIDConverter.fromInt(2),
                                            libraryItemId = UUIDConverter.fromInt(1),
                                            durationSeconds = 120,
                                            startTimestamp = FakeTimeProvider.START_TIME,
                                        ),
                                        libraryItem = expectedLibraryItem
                                    ),
                                    SectionWithLibraryItem(
                                        section = Section(
                                            id = UUIDConverter.fromInt(3),
                                            sessionId = UUIDConverter.fromInt(2),
                                            libraryItemId = UUIDConverter.fromInt(1),
                                            durationSeconds = 60,
                                            startTimestamp = FakeTimeProvider.START_TIME.plus(
                                                5.seconds.toJavaDuration()
                                            ),
                                        ),
                                        libraryItem = expectedLibraryItem
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ))
    }
}