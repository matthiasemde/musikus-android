/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import androidx.test.filters.SmallTest
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.SectionWithLibraryItem
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionModel
import app.musikus.database.entities.SessionUpdateAttributes
import app.musikus.di.AppModule
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@HiltAndroidTest
@UninstallModules(AppModule::class)
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
                LibraryItemModel(
                    name = "TestItem",
                    colorIndex = 1,
                    libraryFolderId = null
                )
            )
        }
    }

    @Test
    fun insertSessions() = runTest {
        sessionDao.insert(listOf(
            SessionModel(
                breakDuration = 10,
                rating = 3,
                comment = "Test comment",
            ),
            SessionModel(
                breakDuration = 20,
                rating = 2,
                comment = "",
            ),
        ))

        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDuration = 10,
                rating = 3,
                comment = "Test comment",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime
            ),
            Session(
                id = UUIDConverter.fromInt(3),
                breakDuration = 20,
                rating = 2,
                comment = "",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime
            )
        )
    }

    @Test
    fun insertSession() = runTest {
        val session = SessionModel(
            breakDuration = 10,
            rating = 3,
            comment = "Test comment",
        )

        val sessionDaoSpy = spyk(sessionDao)

        sessionDaoSpy.insert(session)

        coVerify (exactly = 1) { sessionDaoSpy.insert(listOf(session)) }
    }

    @Test
    fun updateSessions() = runTest {
        sessionDao.insert(listOf(
            SessionModel(
                breakDuration = 10,
                rating = 3,
                comment = "Test comment"
            ),
            SessionModel(
                breakDuration = 20,
                rating = 2,
                comment = "",
            ),
        ))

        fakeTimeProvider.advanceTimeBy(1.seconds)

        sessionDao.update(
            listOf(
                UUIDConverter.fromInt(2) to SessionUpdateAttributes(
                    rating = 5,
                    comment = "",
                ),
                UUIDConverter.fromInt(3) to SessionUpdateAttributes(
                    rating = 1,
                    comment = "Edited comment",
                ),
            )
        )

        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDuration = 10,
                rating = 5,
                comment = "",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration())
            ),
            Session(
                id = UUIDConverter.fromInt(3),
                breakDuration = 20,
                rating = 1,
                comment = "Edited comment",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration())
            )
        )
    }

    @Test
    fun updateSession() = runTest {
        val sessionDaoSpy = spyk(sessionDao)

        try {
            sessionDaoSpy.update(UUIDConverter.fromInt(1), SessionUpdateAttributes())
        } catch (e: Exception) {
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
            "Could not find the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun deleteSessions() = runTest {
        sessionDao.insert(listOf(
            SessionModel(
                breakDuration = 10,
                rating = 3,
                comment = "",
            ),
            SessionModel(
                breakDuration = 20,
                rating = 2,
                comment = "",
            ),
        ))

        sessionDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3),
        ))

        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).isEmpty()
    }

    @Test
    fun deleteSession() = runTest {
        val sessionDaoSpy = spyk(sessionDao)

        try {
            sessionDaoSpy.delete(UUIDConverter.fromInt(1))
        } catch (e: Exception) {
            // ignore
        }

        coVerify (exactly = 1) { sessionDaoSpy.delete(listOf(UUIDConverter.fromInt(1))) }
    }

    @Test
    fun deleteNonExistentItem_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sessionDao.delete(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun restoreItems() = runTest {
        sessionDao.insert(listOf(
            SessionModel(
                breakDuration = 10,
                rating = 3,
                comment = "",
            ),
            SessionModel(
                breakDuration = 20,
                rating = 2,
                comment = "",
            ),
        ))

        sessionDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3),
        ))

        fakeTimeProvider.advanceTimeBy(1.seconds)

        sessionDao.restore(listOf(
                UUIDConverter.fromInt(2),
                UUIDConverter.fromInt(3),
        ))

        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDuration = 10,
                rating = 3,
                comment = "",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration())
            ),
            Session(
                id = UUIDConverter.fromInt(3),
                breakDuration = 20,
                rating = 2,
                comment = "",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration())
            )
        )
    }

    @Test
    fun restoreItem() = runTest {
        val sessionDaoSpy = spyk(sessionDao)

        try {
            sessionDaoSpy.restore(UUIDConverter.fromInt(1))
        } catch (e: Exception) {
            // ignore
        }

        coVerify (exactly = 1) {
            sessionDaoSpy.restore(listOf(UUIDConverter.fromInt(1)))
        }
    }

    @Test
    fun restoreNonExistentItem_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sessionDao.restore(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun getSpecificSessions() = runTest {
        sessionDao.insert(listOf(
            SessionModel(
                breakDuration = 10,
                rating = 3,
                comment = "",
            ),
            SessionModel(
                breakDuration = 20,
                rating = 4,
                comment = "",
            ),
            SessionModel(
                breakDuration = 30,
                rating = 1,
                comment = "",
            ),
        ))

        val sessions = sessionDao.getAsFlow(listOf(
                UUIDConverter.fromInt(4),
                UUIDConverter.fromInt(2),
        )).first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDuration = 10,
                rating = 3,
                comment = "",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime
            ),
            Session(
                id = UUIDConverter.fromInt(4),
                breakDuration = 30,
                rating = 1,
                comment = "",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime
            )
        )
    }

    @Test
    fun getSpecificItem() = runTest {
        val sessionDaoSpy = spyk(sessionDao)

        try {
            sessionDaoSpy.getAsFlow(UUIDConverter.fromInt(2))
        } catch (e: Exception) {
            // ignore
        }

        coVerify (exactly = 1) {
            sessionDaoSpy.getAsFlow(listOf(UUIDConverter.fromInt(2)))
        }
    }

    @Test
    fun getNonExistentItem_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sessionDao.getAsFlow(UUIDConverter.fromInt(4)).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [00000000-0000-0000-0000-000000000004]"
        )
    }

    @Test
    fun sessionExists() = runTest {
        sessionDao.insert(
            SessionModel(
                breakDuration = 10,
                rating = 3,
                comment = "",
            ),
        )

        assertThat(sessionDao.exists(UUIDConverter.fromInt(2))).isTrue()
    }

    @Test
    fun sessionDoesNotExist() = runTest {
        assertThat(sessionDao.exists(UUIDConverter.fromInt(2))).isFalse()
    }

    @Test
    fun cleanSessions() = runTest {
        sessionDao.insert(listOf(
            SessionModel(
                breakDuration = 10,
                rating = 3,
                comment = "",
            ),
            SessionModel(
                breakDuration = 20,
                rating = 2,
                comment = "",
            ),
        ))

        sessionDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3),
        ))

        // advance time by close to a month
        fakeTimeProvider.advanceTimeBy((24 * 28).hours)

        // make sure cleaning does not delete items that are not yet old enough
        sessionDao.clean()

        // if restore works, the items weren't deleted
        sessionDao.restore(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3),
        ))

        sessionDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3),
        ))

        // advance time by more than a month
        fakeTimeProvider.advanceTimeBy((24 * 32).hours)

        // make sure cleaning does delete items that are old enough
        sessionDao.clean()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                // Restoring cleaned items should be impossible
                sessionDao.restore(
                    listOf(
                        UUIDConverter.fromInt(2),
                        UUIDConverter.fromInt(3)
                    )
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [" +
                    "00000000-0000-0000-0000-000000000002, " +
                    "00000000-0000-0000-0000-000000000003" +
                    "]"
        )
    }

    @Test fun insertSessionWithSections() = runTest {
        val session = SessionModel(
            breakDuration = 10,
            rating = 3,
            comment = "Test comment",
        )

        val sectionCreationAttributes = listOf(
            SectionCreationAttributes(
                libraryItemId = Nullable(UUIDConverter.fromInt(1)),
                duration = 60,
                startTimestamp = fakeTimeProvider.now(),
            ),
            SectionCreationAttributes(
                libraryItemId = Nullable(UUIDConverter.fromInt(1)),
                duration = 150,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        sessionDao.insert(session, sectionCreationAttributes)

        val sessions = sessionDao.getAllAsFlow().first()

        assertThat(sessions).containsExactly(
            Session(
                id = UUIDConverter.fromInt(2),
                breakDuration = 10,
                rating = 3,
                comment = "Test comment",
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime
            )
        )

        val sections = database.sectionDao.getAllAsFlow().first()

        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                duration = 60,
                startTimestamp = fakeTimeProvider.startTime,
            ),
            Section(
                id = UUIDConverter.fromInt(4),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                duration = 150,
                startTimestamp = fakeTimeProvider.startTime,
            )
        )
    }

    private suspend fun insertSessionWithSection() {
        val session = SessionModel(
            breakDuration = 10,
            rating = 3,
            comment = "",
        )

        val sectionCreationAttributes = listOf(
            SectionCreationAttributes(
                libraryItemId = Nullable(UUIDConverter.fromInt(1)),
                duration = 60,
                startTimestamp = fakeTimeProvider.now(),
            )
        )

        sessionDao.insert(
            session,
            sectionCreationAttributes
        )
    }

    @Test
    fun getSessionsWithSectionsWithLibraryItems() = runTest {
        insertSessionWithSection()

        val sessionsWithSectionsWithLibraryItems = sessionDao.getAllWithSectionsWithLibraryItemsAsFlow().first()

        assertThat(sessionsWithSectionsWithLibraryItems).containsExactly(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(2),
                    breakDuration = 10,
                    rating = 3,
                    comment = "",
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(3),
                            sessionId = UUIDConverter.fromInt(2),
                            libraryItemId = UUIDConverter.fromInt(1),
                            duration = 60,
                            startTimestamp = fakeTimeProvider.startTime,
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(1),
                            name = "TestItem",
                            colorIndex = 1,
                            libraryFolderId = null,
                            customOrder = null,
                            createdAt = fakeTimeProvider.startTime,
                            modifiedAt = fakeTimeProvider.startTime
                        )
                    )
                )
            )
        )
    }

    @Test
    fun getWithSectionsWithLibraryItemsAsFlow() = runTest {
        insertSessionWithSection()

        val sessionWithSectionsWithLibraryItems = sessionDao.getWithSectionsWithLibraryItems(
            UUIDConverter.fromInt(2)
        )

        assertThat(sessionWithSectionsWithLibraryItems).isEqualTo(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(2),
                    breakDuration = 10,
                    rating = 3,
                    comment = "",
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(3),
                            sessionId = UUIDConverter.fromInt(2),
                            libraryItemId = UUIDConverter.fromInt(1),
                            duration = 60,
                            startTimestamp = fakeTimeProvider.startTime,
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(1),
                            name = "TestItem",
                            colorIndex = 1,
                            libraryFolderId = null,
                            customOrder = null,
                            createdAt = fakeTimeProvider.startTime,
                            modifiedAt = fakeTimeProvider.startTime
                        )
                    )
                )
            )
        )
    }

    @Test
    fun getWithSectionsWithLibraryItemsAsFlow_nonExistentSession() = runTest {
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
            startTimestamp = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            endTimestamp = fakeTimeProvider.startTime.plus(2.seconds.toJavaDuration())
        ).first()

        assertThat(sessions).containsExactly(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(4),
                    breakDuration = 10,
                    rating = 3,
                    comment = "",
                    createdAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
                    modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration())
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(5),
                            sessionId = UUIDConverter.fromInt(4),
                            libraryItemId = UUIDConverter.fromInt(1),
                            duration = 60,
                            startTimestamp = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(1),
                            name = "TestItem",
                            colorIndex = 1,
                            libraryFolderId = null,
                            customOrder = null,
                            createdAt = fakeTimeProvider.startTime,
                            modifiedAt = fakeTimeProvider.startTime
                        )
                    )
                )
            )
        )
    }
}