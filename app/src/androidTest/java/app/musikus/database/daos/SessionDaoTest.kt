/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import android.database.sqlite.SQLiteConstraintException
import androidx.test.filters.SmallTest
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.data.Nullable
import app.musikus.core.data.SectionWithLibraryItem
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import app.musikus.sessions.data.entities.SessionUpdateAttributes
import app.musikus.library.data.daos.LibraryItem
import app.musikus.sessions.data.daos.Section
import app.musikus.sessions.data.daos.Session
import app.musikus.sessions.data.daos.SessionDao
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@HiltAndroidTest
@SmallTest
class SessionDaoTest {

    @Inject
    @Named("test_db")
    lateinit var database: MusikusDatabase
    private lateinit var sessionDao: SessionDao

    @Inject
    lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()

        sessionDao = database.sessionDao

        // insert a library item so we can use it in test sessions
        runBlocking {
            database.libraryItemDao.insert(
                LibraryItemCreationAttributes(
                    name = "TestItem",
                    colorIndex = 1,
                    libraryFolderId = Nullable(null)
                )
            )
        }
    }

    private suspend fun insertSessionWithSection() {
        val session = SessionCreationAttributes(
            breakDuration = 10.minutes,
            rating = 3,
            comment = "Test comment",
        )

        val sectionCreationAttributes = listOf(
            SectionCreationAttributes(
                libraryItemId = UUIDConverter.fromInt(1),
                duration = 15.minutes,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        sessionDao.insert(
            session,
            sectionCreationAttributes
        )
    }

    @Test
    fun insertSessions_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                sessionDao.insert(listOf(
                    SessionCreationAttributes(
                    breakDuration = 10.seconds,
                    rating = 3,
                    comment = "Test comment"
                )
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Use insert(sessionCreationAttributes, sectionCreationAttributes) instead"
        )
    }

    @Test
    fun insertSession_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                sessionDao.insert(
                    SessionCreationAttributes(
                    breakDuration = 10.seconds,
                    rating = 3,
                    comment = "Test comment"
                )
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Use insert(sessionCreationAttributes, sectionCreationAttributes) instead"
        )
    }

    @Test
    fun insertSessions() = runTest {
        val sessionCreationAttributes = SessionCreationAttributes(
            breakDuration = 10.minutes,
            rating = 3,
            comment = "Test comment",
        )

        val sectionCreationAttributes = listOf(
            SectionCreationAttributes(
                libraryItemId = UUIDConverter.fromInt(1),
                duration = 15.minutes,
                startTimestamp = fakeTimeProvider.now(),
            ),
            SectionCreationAttributes(
                libraryItemId = UUIDConverter.fromInt(1),
                duration = 25.minutes,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        val (sessionId, sectionIds) = sessionDao.insert(
            sessionCreationAttributes,
            sectionCreationAttributes
        )

        // Check if session and section ids were returned correctly
        assertThat(sessionId).isEqualTo(UUIDConverter.fromInt(2))
        assertThat(sectionIds).containsExactly(
            UUIDConverter.fromInt(3),
            UUIDConverter.fromInt(4)
        )

        // Check if session and sections were inserted correctly
        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDurationSeconds = 600,
                rating = 3,
                comment = "Test comment",
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME
            )
        )

        val sections = database.sectionDao.getAllAsFlow().first()

        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 900,
                startTimestamp = FakeTimeProvider.START_TIME,
            ),
            Section(
                id = UUIDConverter.fromInt(4),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 1500,
                startTimestamp = FakeTimeProvider.START_TIME,
            )
        )
    }

    @Test
    fun updateSessions() = runTest {
        repeat(2) { insertSessionWithSection() }

        fakeTimeProvider.advanceTimeBy(1.seconds)

        sessionDao.update(
            listOf(
                UUIDConverter.fromInt(2) to SessionUpdateAttributes(
                    rating = 5,
                    comment = "",
                ),
                UUIDConverter.fromInt(4) to SessionUpdateAttributes(
                    rating = 1,
                    comment = "Edited comment",
                ),
            )
        )

        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDurationSeconds = 600,
                rating = 5,
                comment = "",
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration())
            ),
            Session(
                id = UUIDConverter.fromInt(4),
                breakDurationSeconds = 600,
                rating = 1,
                comment = "Edited comment",
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration())
            )
        )
    }

    @Test
    fun updateSession() = runTest {
        val sessionDaoSpy = spyk(sessionDao)

        try {
            sessionDaoSpy.update(UUIDConverter.fromInt(1), SessionUpdateAttributes())
        } catch (e: IllegalArgumentException) {
            // ignore
        }

        coVerify (exactly = 1) {
            sessionDaoSpy.update(UUIDConverter.fromInt(1), SessionUpdateAttributes())
        }
    }

    @Test
    fun updateNonExistentSession_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sessionDao.update(
                    UUIDConverter.fromInt(0),
                    SessionUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find session(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun deleteSessions() = runTest {
        repeat(2) { insertSessionWithSection() }

        sessionDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(4),
        ))

        // check if sessions were deleted correctly
        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).isEmpty()

        // check if sections were deleted correctly as well
        val sections = database.sectionDao.getAllAsFlow().first()

        assertThat(sections).isEmpty()
    }

    @Test
    fun deleteSession() = runTest {
        val sessionDaoSpy = spyk(sessionDao)

        try {
            sessionDaoSpy.delete(UUIDConverter.fromInt(1))
        } catch (e: IllegalArgumentException) {
            // ignore
        }

        coVerify (exactly = 1) { sessionDaoSpy.delete(listOf(UUIDConverter.fromInt(1))) }
    }

    @Test
    fun deleteNonExistentSession_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sessionDao.delete(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find session(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun restoreSessions() = runTest {
        repeat(2) { insertSessionWithSection() }

        sessionDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(4),
        ))

        fakeTimeProvider.advanceTimeBy(1.seconds)

        sessionDao.restore(listOf(
                UUIDConverter.fromInt(2),
                UUIDConverter.fromInt(4),
        ))

        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDurationSeconds = 600,
                rating = 3,
                comment = "Test comment",
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration())
            ),
            Session(
                id = UUIDConverter.fromInt(4),
                breakDurationSeconds = 600,
                rating = 3,
                comment = "Test comment",
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration())
            )
        )

        // check if sections were restored correctly as well
        val sections = database.sectionDao.getAllAsFlow().first()

        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 900,
                startTimestamp = FakeTimeProvider.START_TIME,
            ),
            Section(
                id = UUIDConverter.fromInt(5),
                sessionId = UUIDConverter.fromInt(4),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 900,
                startTimestamp = FakeTimeProvider.START_TIME,
            )
        )
    }

    @Test
    fun restoreSession() = runTest {
        val sessionDaoSpy = spyk(sessionDao)

        try {
            sessionDaoSpy.restore(UUIDConverter.fromInt(1))
        } catch (e: IllegalArgumentException) {
            // ignore
        }

        coVerify (exactly = 1) {
            sessionDaoSpy.restore(listOf(UUIDConverter.fromInt(1)))
        }
    }

    @Test
    fun restoreNonExistentSession_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sessionDao.restore(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find session(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun getSpecificSessions() = runTest {
        repeat(3) { insertSessionWithSection() }

        val sessions = sessionDao.getAsFlow(listOf(
                UUIDConverter.fromInt(2),
                UUIDConverter.fromInt(6),
        )).first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDurationSeconds = 600,
                rating = 3,
                comment = "Test comment",
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME
            ),
            Session(
                id = UUIDConverter.fromInt(6),
                breakDurationSeconds = 600,
                rating = 3,
                comment = "Test comment",
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME
            )
        )
    }

    @Test
    fun getSpecificSession() = runTest {
        val sessionDaoSpy = spyk(sessionDao)

        try {
            sessionDaoSpy.getAsFlow(UUIDConverter.fromInt(2))
        } catch (e: IllegalArgumentException) {
            // ignore
        }

        coVerify (exactly = 1) {
            sessionDaoSpy.getAsFlow(listOf(UUIDConverter.fromInt(2)))
        }
    }

    @Test
    fun getNonExistentSession_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sessionDao.getAsFlow(UUIDConverter.fromInt(4)).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find session(s) with the following id(s): [00000000-0000-0000-0000-000000000004]"
        )
    }

    @Test
    fun sessionExists() = runTest {
        insertSessionWithSection()

        assertThat(sessionDao.exists(UUIDConverter.fromInt(2))).isTrue()
    }

    @Test
    fun sessionDoesNotExist() = runTest {
        assertThat(sessionDao.exists(UUIDConverter.fromInt(2))).isFalse()
    }

    @Test
    fun cleanSessions() = runTest {
        repeat(2) { insertSessionWithSection() }

        sessionDao.delete(UUIDConverter.fromInt(2))

        // advance time by a few days
        fakeTimeProvider.advanceTimeBy(4.days)

        sessionDao.delete(UUIDConverter.fromInt(4))

        // advance time by just under a month and clean sessions
        fakeTimeProvider.advanceTimeBy(28.days)

        sessionDao.clean()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                // Restoring session with id 2 should be impossible to
                // restore since it was cleaned
                // session with id 4 should be restored
                sessionDao.restore(
                    listOf(
                        UUIDConverter.fromInt(2),
                        UUIDConverter.fromInt(4)
                    )
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find session(s) with the following id(s): [00000000-0000-0000-0000-000000000002]"
        )

        // finally, check whether session 2's section was removed as per foreign key constraint
        assertThat(database.sectionDao.exists(UUIDConverter.fromInt(3))).isFalse()
    }

    @Test fun insertSessionWithSections() = runTest {
        val session = SessionCreationAttributes(
            breakDuration = 10.minutes,
            rating = 3,
            comment = "Test comment",
        )

        val sectionCreationAttributes = listOf(
            SectionCreationAttributes(
                libraryItemId = UUIDConverter.fromInt(1),
                duration = 5.minutes,
                startTimestamp = fakeTimeProvider.now(),
            ),
            SectionCreationAttributes(
                libraryItemId = UUIDConverter.fromInt(1),
                duration = 15.minutes,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        sessionDao.insert(session, sectionCreationAttributes)

        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDurationSeconds = 600,
                rating = 3,
                comment = "Test comment",
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME
            )
        )

        val sections = database.sectionDao.getAllAsFlow().first()

        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 300,
                startTimestamp = FakeTimeProvider.START_TIME,
            ),
            Section(
                id = UUIDConverter.fromInt(4),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 900,
                startTimestamp = FakeTimeProvider.START_TIME,
            )
        )
    }

    @Test
    fun insertSessionWithSectionWithInvalidLibraryItemId_throwsException() = runTest {
        val exception = assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                sessionDao.insert(
                    SessionCreationAttributes(
                        breakDuration = 10.minutes,
                        rating = 3,
                        comment = "Test comment",
                    ),
                    sectionCreationAttributes = listOf(
                        SectionCreationAttributes(
                            libraryItemId = UUIDConverter.fromInt(0),
                            duration = 10.minutes,
                            startTimestamp = FakeTimeProvider.START_TIME
                        )
                    )
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)"
        )
    }

    @Test
    fun getOrderedWithSectionsWithLibraryItems() = runTest {

        // insert three unordered sessions
        sessionDao.insert(
            sessionCreationAttributes = SessionCreationAttributes(
                breakDuration = 10.minutes,
                rating = 3,
                comment = "Test comment",
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 2.minutes,
                    startTimestamp = fakeTimeProvider.now(),
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 4.minutes,
                    startTimestamp = fakeTimeProvider.also { it.advanceTimeBy(2.minutes) }.now(),
                )
            )
        )

        fakeTimeProvider.advanceTimeBy(1.days)

        sessionDao.insert(
            sessionCreationAttributes = SessionCreationAttributes(
                breakDuration = 10.minutes,
                rating = 3,
                comment = "Test comment",
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 3.minutes,
                    startTimestamp = fakeTimeProvider.now(),
                )
            )
        )

        // place the last session between the two sections of the first session to see if it gets sorted correctly
        fakeTimeProvider.setCurrentDateTime(
            FakeTimeProvider.START_TIME.plus(1.minutes.toJavaDuration())
        )

        sessionDao.insert(
            sessionCreationAttributes = SessionCreationAttributes(
                breakDuration = 10.minutes,
                rating = 3,
                comment = "Test comment",
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 5.minutes,
                    startTimestamp = fakeTimeProvider.now(),
                ),
            )
        )

        val sessionsWithSectionsWithLibraryItems = sessionDao.getOrderedWithSectionsWithLibraryItems().first()

        val expectedItem = LibraryItem(
            id = UUIDConverter.fromInt(1),
            name = "TestItem",
            colorIndex = 1,
            libraryFolderId = null,
            customOrder = null,
            createdAt = FakeTimeProvider.START_TIME,
            modifiedAt = FakeTimeProvider.START_TIME
        )

        assertThat(sessionsWithSectionsWithLibraryItems).isEqualTo(listOf(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(5),
                    breakDurationSeconds = 600,
                    rating = 3,
                    comment = "Test comment",
                    createdAt = FakeTimeProvider.START_TIME.plus(
                        (2.minutes + 1.days).toJavaDuration()
                    ),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(
                        (2.minutes + 1.days).toJavaDuration()
                    )
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(6),
                            sessionId = UUIDConverter.fromInt(5),
                            libraryItemId = UUIDConverter.fromInt(1),
                            durationSeconds = 180,
                            startTimestamp = FakeTimeProvider.START_TIME.plus(
                                (2.minutes + 1.days).toJavaDuration()
                            ),
                        ),
                        libraryItem = expectedItem
                    )
                )
            ),
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(7),
                    breakDurationSeconds = 600,
                    rating = 3,
                    comment = "Test comment",
                    createdAt = FakeTimeProvider.START_TIME.plus(
                        (1.minutes).toJavaDuration()
                    ),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(
                        (1.minutes).toJavaDuration()
                    )
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(8),
                            sessionId = UUIDConverter.fromInt(7),
                            libraryItemId = UUIDConverter.fromInt(1),
                            durationSeconds = 300,
                            startTimestamp = FakeTimeProvider.START_TIME.plus(
                                (1.minutes).toJavaDuration()
                            ),
                        ),
                        libraryItem = expectedItem
                    )
                )
            ),
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(2),
                    breakDurationSeconds = 600,
                    rating = 3,
                    comment = "Test comment",
                    createdAt = FakeTimeProvider.START_TIME.plus(
                        (2.minutes).toJavaDuration()
                    ),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(
                        (2.minutes).toJavaDuration()
                    )
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(3),
                            sessionId = UUIDConverter.fromInt(2),
                            libraryItemId = UUIDConverter.fromInt(1),
                            durationSeconds = 120,
                            startTimestamp = FakeTimeProvider.START_TIME,
                        ),
                        libraryItem = expectedItem
                    ),
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(4),
                            sessionId = UUIDConverter.fromInt(2),
                            libraryItemId = UUIDConverter.fromInt(1),
                            durationSeconds = 240,
                            startTimestamp = FakeTimeProvider.START_TIME.plus(
                                (2.minutes).toJavaDuration()
                            ),
                        ),
                        libraryItem = expectedItem
                    )
                )
            )
        ))
    }

    @Test
    fun getWithSectionsWithLibraryItems() = runTest {
        repeat(2) { insertSessionWithSection() }

        val sessionWithSectionsWithLibraryItems = sessionDao.getWithSectionsWithLibraryItems(
            UUIDConverter.fromInt(2)
        )

        assertThat(sessionWithSectionsWithLibraryItems).isEqualTo(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(2),
                    breakDurationSeconds = 600,
                    rating = 3,
                    comment = "Test comment",
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(3),
                            sessionId = UUIDConverter.fromInt(2),
                            libraryItemId = UUIDConverter.fromInt(1),
                            durationSeconds = 900,
                            startTimestamp = FakeTimeProvider.START_TIME,
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(1),
                            name = "TestItem",
                            colorIndex = 1,
                            libraryFolderId = null,
                            customOrder = null,
                            createdAt = FakeTimeProvider.START_TIME,
                            modifiedAt = FakeTimeProvider.START_TIME
                        )
                    )
                )
            )
        )
    }

    @Test
    fun getWithSectionsWithLibraryItems_nonExistentSession() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sessionDao.getWithSectionsWithLibraryItems(
                    UUIDConverter.fromInt(2)
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Session with id 00000000-0000-0000-0000-000000000002 not found"
        )
    }


    @Test
    fun getFromTimeFrame() = runTest {
        insertSessionWithSection()

        fakeTimeProvider.advanceTimeBy(1.seconds)

        insertSessionWithSection()

        fakeTimeProvider.advanceTimeBy(1.seconds)

        insertSessionWithSection()

        val sessions = sessionDao.getFromTimeframe(
            FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()) to
            FakeTimeProvider.START_TIME.plus(2.seconds.toJavaDuration())
        ).first()

        assertThat(sessions).containsExactly(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(4),
                    breakDurationSeconds = 600,
                    rating = 3,
                    comment = "Test comment",
                    createdAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration())
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(5),
                            sessionId = UUIDConverter.fromInt(4),
                            libraryItemId = UUIDConverter.fromInt(1),
                            durationSeconds = 900,
                            startTimestamp = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(1),
                            name = "TestItem",
                            colorIndex = 1,
                            libraryFolderId = null,
                            customOrder = null,
                            createdAt = FakeTimeProvider.START_TIME,
                            modifiedAt = FakeTimeProvider.START_TIME
                        )
                    )
                )
            )
        )
    }
}