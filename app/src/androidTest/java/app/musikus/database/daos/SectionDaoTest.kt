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
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SectionUpdateAttributes
import app.musikus.database.entities.SessionCreationAttributes
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration


@HiltAndroidTest
@SmallTest
class SectionDaoTest {

    @Inject
    @Named("test_db")
    lateinit var database: MusikusDatabase
    private lateinit var sectionDao: SectionDao

    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()

        sectionDao = database.sectionDao

        runBlocking {
            database.libraryItemDao.insert(
                LibraryItemCreationAttributes(
                    name = "TestItem1",
                    colorIndex = 6,
                    libraryFolderId = Nullable(null),
                ),
            )

            database.sessionDao.insert(
                sessionCreationAttributes = SessionCreationAttributes(
                    breakDuration = 2.minutes,
                    rating = 2,
                    comment = "Test comment",
                ),
                sectionCreationAttributes = listOf(
                    SectionCreationAttributes(
                        libraryItemId = UUIDConverter.fromInt(1),
                        duration = 2.minutes,
                        startTimestamp = fakeTimeProvider.now()
                    ),
                    SectionCreationAttributes(
                        libraryItemId = UUIDConverter.fromInt(1),
                        duration = 4.minutes,
                        startTimestamp = fakeTimeProvider.apply {
                            advanceTimeBy(2.minutes)
                        }.now()
                    )
                )
            )

            // revert the time to the start time of the session
            fakeTimeProvider.revertTimeBy(2.minutes)
        }
    }

    @Test
    fun getAll() = runTest {
        // Get the sections
        val sections = sectionDao.getAllAsFlow().first()

        // Check if the sections were retrieved correctly
        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 120,
                startTimestamp = FakeTimeProvider.START_TIME,
            ),
            Section(
                id = UUIDConverter.fromInt(4),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 240,
                startTimestamp = FakeTimeProvider.START_TIME.plus(2.minutes.toJavaDuration()),
            )
        )
    }

    @Test
    fun getAllWithDeletedSession() = runTest {
        // Delete the session
        database.sessionDao.delete(UUIDConverter.fromInt(2))

        // Get the sections
        val sections = sectionDao.getAllAsFlow().first()

        // Check if the sections were retrieved correctly
        assertThat(sections).isEmpty()
    }

    @Test
    fun insertSection_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                sectionDao.insert(SectionCreationAttributes(
                    sessionId = UUIDConverter.fromInt(2),
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 10.minutes,
                    startTimestamp = FakeTimeProvider.START_TIME
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Sections are inserted only in conjunction with their session"
        )
    }

    @Test
    fun updateSections() = runTest {
        // Update the sections
        sectionDao.update(
            listOf(
                UUIDConverter.fromInt(3) to SectionUpdateAttributes(
                    duration = 5.minutes,
                ),
                UUIDConverter.fromInt(4) to SectionUpdateAttributes(
                    duration = 1.hours,
                )
            )
        )

        // Check if the sections were updated correctly
        val updatedSections = sectionDao.getAllAsFlow().first()

        assertThat(updatedSections).containsExactly(
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
                durationSeconds = 3600,
                startTimestamp = FakeTimeProvider.START_TIME.plus(2.minutes.toJavaDuration()),
            )
        )
    }

    @Test
    fun updateSection() = runTest {
        val updateAttributes = SectionUpdateAttributes(duration = 5.minutes)

        val sectionDaoSpy = spyk(sectionDao)

        try {
            sectionDaoSpy.update(
                UUIDConverter.fromInt(1),
                updateAttributes
            )
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify (exactly = 1) {
            sectionDaoSpy.update(listOf(
                UUIDConverter.fromInt(1) to updateAttributes
            ))
        }
    }

    @Test
    fun updateNonExistentSection_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sectionDao.update(
                    UUIDConverter.fromInt(0),
                    SectionUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find section(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun updateSectionOfDeletedSession_throwsException() = runTest {
        // Delete the session
        database.sessionDao.delete(UUIDConverter.fromInt(2))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sectionDao.update(
                    UUIDConverter.fromInt(3),
                    SectionUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find section(s) with the following id(s): [00000000-0000-0000-0000-000000000003]"
        )
    }

    @Test
    fun deleteSections_throwsNotImplementedError() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                sectionDao.delete(listOf(
                    UUIDConverter.fromInt(1),
                    UUIDConverter.fromInt(2)
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Sections are automatically deleted when their session is deleted"
        )
    }

    @Test
    fun deleteSection() = runTest {
        val exception = assertThrows(NotImplementedError::class.java) {
            runBlocking {
                sectionDao.delete(UUIDConverter.fromInt(1))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Sections are automatically deleted when their session is deleted"
        )
    }

    @Test
    fun getSpecificSections() = runTest {
        // Insert another session with a few more sections
        database.sessionDao.insert(
            sessionCreationAttributes = SessionCreationAttributes(
                breakDuration = 3.minutes,
                rating = 3,
                comment = "Test comment2",
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 3.minutes,
                    startTimestamp = fakeTimeProvider.now()
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 9.minutes,
                    startTimestamp = fakeTimeProvider.apply {
                        advanceTimeBy(3.minutes)
                    }.now()
                )
            )
        )

        // Get the sections
        val sections = sectionDao.getAsFlow(listOf(
            UUIDConverter.fromInt(3),
            UUIDConverter.fromInt(7)
        )).first()

        // Check if the sections were retrieved correctly
        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 120,
                startTimestamp = FakeTimeProvider.START_TIME,
            ),
            Section(
                id = UUIDConverter.fromInt(7),
                sessionId = UUIDConverter.fromInt(5),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 540,
                startTimestamp = FakeTimeProvider.START_TIME.plus(3.minutes.toJavaDuration()),
            )
        )
    }

    @Test
    fun getSpecificSection() = runTest {
        val sectionDaoSpy = spyk(sectionDao)

        sectionDaoSpy.getAsFlow(UUIDConverter.fromInt(3))

        coVerify (exactly = 1) {
            sectionDaoSpy.getAsFlow(listOf(UUIDConverter.fromInt(3)))
        }
    }

    @Test
    fun getNonExistentSection_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sectionDao.getAsFlow(UUIDConverter.fromInt(1)).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find section(s) with the following id(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun getSpecificSectionOfDeletedGoal_throwsException() = runTest {
        // Delete the session
        database.sessionDao.delete(UUIDConverter.fromInt(2))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sectionDao.getAsFlow(UUIDConverter.fromInt(3)).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find section(s) with the following id(s): [00000000-0000-0000-0000-000000000003]"
        )
    }

    @Test
    fun sectionExists() = runTest {
        // Check if the section exists
        assertThat(sectionDao.exists(UUIDConverter.fromInt(3))).isTrue()
    }

    @Test
    fun sectionOfDeletedSessionDoesNotExist() = runTest {
        // Delete the session
        database.sessionDao.delete(UUIDConverter.fromInt(2))

        // Check if the section exists
        assertThat(sectionDao.exists(UUIDConverter.fromInt(3))).isFalse()
    }

    @Test
    fun sectionDoesNotExist() = runTest {
        assertThat(sectionDao.exists(UUIDConverter.fromInt(1))).isFalse()
    }

    @Test
    fun getSectionsForSession() = runTest {
        // Get the sections
        val sections = sectionDao.getForSession(UUIDConverter.fromInt(2))

        // Check if the sections were retrieved correctly
        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 120,
                startTimestamp = FakeTimeProvider.START_TIME,
            ),
            Section(
                id = UUIDConverter.fromInt(4),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 240,
                startTimestamp = FakeTimeProvider.START_TIME.plus(2.minutes.toJavaDuration()),
            )
        )
    }

    @Test
    fun getSectionsInTimeframe() = runTest {
        // Insert another session with a few more sections
        database.sessionDao.insert(
            sessionCreationAttributes = SessionCreationAttributes(
                breakDuration = 3.minutes,
                rating = 3,
                comment = "Test comment2",
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 3.minutes,
                    startTimestamp = fakeTimeProvider.now()
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 9.minutes,
                    startTimestamp = fakeTimeProvider.apply {
                        advanceTimeBy(3.minutes)
                    }.now()
                )
            )
        )

        // Get the sections
        val sections = sectionDao.getInTimeframe(
            startTimestamp = FakeTimeProvider.START_TIME,
            endTimestamp = FakeTimeProvider.START_TIME.plus(3.minutes.toJavaDuration())
        ).first()

        // Check if the sections were retrieved correctly
        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 120,
                startTimestamp = FakeTimeProvider.START_TIME,
            ),
            Section(
                id = UUIDConverter.fromInt(4),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 240,
                startTimestamp = FakeTimeProvider.START_TIME.plus(2.minutes.toJavaDuration()),
            ),
            Section(
                id = UUIDConverter.fromInt(6),
                sessionId = UUIDConverter.fromInt(5),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 180,
                startTimestamp = FakeTimeProvider.START_TIME,
            )
        )
    }

    @Test
    fun getSectionsInTimeframeWithDeletedSession_noSections() = runTest {
        // Delete the session
        database.sessionDao.delete(UUIDConverter.fromInt(2))

        // Get the sections
        val sections = sectionDao.getInTimeframe(
            startTimestamp = FakeTimeProvider.START_TIME,
            endTimestamp = FakeTimeProvider.START_TIME.plus(15.minutes.toJavaDuration()),
        ).first()

        // Check if the sections were retrieved correctly
        assertThat(sections).isEmpty()
    }

    @Test
    fun getSectionsInTimeframeForItemId() = runTest {
        // Insert more library items
        database.libraryItemDao.insert(listOf(
            LibraryItemCreationAttributes(
                name = "TestItem2",
                colorIndex = 6,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemCreationAttributes(
                name = "TestItem3",
                colorIndex = 7,
                libraryFolderId = Nullable(null),
            ),
        ))

        // Insert another session with a few more sections
        database.sessionDao.insert(
            sessionCreationAttributes = SessionCreationAttributes(
                breakDuration = 3.minutes,
                rating = 3,
                comment = "Test comment2",
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(6),
                    duration = 3.minutes,
                    startTimestamp = fakeTimeProvider.now()
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(5),
                    duration = 9.minutes,
                    startTimestamp = fakeTimeProvider.apply {
                        advanceTimeBy(3.minutes)
                    }.now()
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    duration = 27.minutes,
                    startTimestamp = fakeTimeProvider.apply {
                        advanceTimeBy(9.minutes)
                    }.now()
                )
            )
        )

        // Get the sections
        val sections = sectionDao.getInTimeframeForItemId(
            startTimestamp = FakeTimeProvider.START_TIME,
            endTimestamp = FakeTimeProvider.START_TIME.plus(12.minutes.toJavaDuration()),
            itemIds = listOf(
                UUIDConverter.fromInt(1),
                UUIDConverter.fromInt(5),
            )
        ).first()

        // Check if the sections were retrieved correctly
        assertThat(sections).containsExactly(
            Section(
                id = UUIDConverter.fromInt(3),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 120,
                startTimestamp = FakeTimeProvider.START_TIME,
            ),
            Section(
                id = UUIDConverter.fromInt(4),
                sessionId = UUIDConverter.fromInt(2),
                libraryItemId = UUIDConverter.fromInt(1),
                durationSeconds = 240,
                startTimestamp = FakeTimeProvider.START_TIME.plus(2.minutes.toJavaDuration()),
            ),
            Section(
                id = UUIDConverter.fromInt(9),
                sessionId = UUIDConverter.fromInt(7),
                libraryItemId = UUIDConverter.fromInt(5),
                durationSeconds = 540,
                startTimestamp = FakeTimeProvider.START_TIME.plus(3.minutes.toJavaDuration()),
            )
        )
    }

    @Test
    fun getSectionsInTimeframeForItemIdWithInvalidItemId_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                sectionDao.getInTimeframeForItemId(
                    startTimestamp = FakeTimeProvider.START_TIME.plus(5.minutes.toJavaDuration()),
                    endTimestamp = FakeTimeProvider.START_TIME.plus(15.minutes.toJavaDuration()),
                    itemIds = listOf(
                        UUIDConverter.fromInt(1),
                        UUIDConverter.fromInt(2),
                    )
                ).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find library_item(s) with the following id(s): [00000000-0000-0000-0000-000000000002]"
        )
    }

    @Test
    fun getSectionsInTimeframeForItemIdWithDeletedSession_noSections() = runTest {
        // Delete the session
        database.sessionDao.delete(UUIDConverter.fromInt(2))

        // Get the sections
        val sections = sectionDao.getInTimeframeForItemId(
            startTimestamp = FakeTimeProvider.START_TIME,
            endTimestamp = FakeTimeProvider.START_TIME.plus(15.minutes.toJavaDuration()),
            itemIds = listOf(
                UUIDConverter.fromInt(1),
            )
        ).first()

        // Check if the sections were retrieved correctly
        assertThat(sections).isEmpty()
    }
}