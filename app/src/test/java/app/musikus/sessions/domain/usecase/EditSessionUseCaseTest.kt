/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.domain.usecase

import app.musikus.core.data.SectionWithLibraryItem
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.sessions.data.FakeSessionRepository
import app.musikus.sessions.data.daos.InvalidSectionException
import app.musikus.sessions.data.daos.InvalidSessionException
import app.musikus.sessions.data.daos.Section
import app.musikus.sessions.data.daos.Session
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SectionUpdateAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import app.musikus.sessions.data.entities.SessionUpdateAttributes
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.minutes

class EditSessionUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeSessionRepository: FakeSessionRepository

    /** SUT */
    private lateinit var editSessionUseCase: EditSessionUseCase

    private val validSessionUpdateAttributes = SessionUpdateAttributes(
        rating = 5,
        comment = "Edited comment"
    )

    private val validSectionUpdateData = (3..4).map {
        UUIDConverter.fromInt(it) to SectionUpdateAttributes(duration = 15.minutes)
    }

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeSessionRepository = FakeSessionRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        /** SUT */
        editSessionUseCase = EditSessionUseCase(fakeSessionRepository)

        runBlocking {
            fakeLibraryRepository.addItem(
                LibraryItemCreationAttributes(
                    name = "Test item 1",
                    colorIndex = 5,
                )
            )
            fakeSessionRepository.add(
                sessionCreationAttributes = SessionCreationAttributes(
                    rating = 3,
                    breakDuration = 10.minutes,
                    comment = "Test comment"
                ),
                sectionCreationAttributes = listOf(
                    SectionCreationAttributes(
                        libraryItemId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.now(),
                        duration = 10.minutes
                    ),
                    SectionCreationAttributes(
                        libraryItemId = UUIDConverter.fromInt(1),
                        startTimestamp = fakeTimeProvider.now(),
                        duration = 10.minutes
                    )
                )
            )
        }
    }

    @Test
    fun `Edit session with non-existent id, IllegalArgumentException`() = runBlocking {
        val exception = assertThrows<IllegalArgumentException> {
            editSessionUseCase(
                id = UUIDConverter.fromInt(0),
                sessionUpdateAttributes = validSessionUpdateAttributes,
                sectionUpdateData = validSectionUpdateData
            )
        }

        assertThat(exception.message).isEqualTo("Session with id 00000000-0000-0000-0000-000000000000 does not exist")
    }

    @Test
    fun `Edit session with invalid rating, InvalidSessionException`() = runBlocking {
        val exception = assertThrows<InvalidSessionException> {
            editSessionUseCase(
                id = UUIDConverter.fromInt(2),
                sessionUpdateAttributes = validSessionUpdateAttributes.copy(rating = 6),
                sectionUpdateData = validSectionUpdateData
            )
        }

        assertThat(exception.message).isEqualTo("Rating must be between 1 and 5")
    }

    @Test
    fun `Edit session with invalid comment, InvalidSessionException`() = runBlocking {
        val exception = assertThrows<InvalidSessionException> {
            editSessionUseCase(
                id = UUIDConverter.fromInt(2),
                sessionUpdateAttributes = validSessionUpdateAttributes.copy(comment = "a".repeat(501)),
                sectionUpdateData = validSectionUpdateData
            )
        }

        assertThat(exception.message).isEqualTo("Comment must be less than 500 characters")
    }

    @Test
    fun `Edit session with non-existent or foreign section id, IllegalArgumentException`() = runBlocking {
        fakeSessionRepository.add(
            sessionCreationAttributes = SessionCreationAttributes(
                rating = 1,
                breakDuration = 1.minutes,
                comment = "Test comment"
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = fakeTimeProvider.now(),
                    duration = 10.minutes
                )
            )
        )

        val allSections = fakeSessionRepository.sections.first()

        assertThat(allSections.map { it.id }).contains(UUIDConverter.fromInt(6))

        val exception = assertThrows<IllegalArgumentException> {
            editSessionUseCase(
                id = UUIDConverter.fromInt(2),
                sessionUpdateAttributes = validSessionUpdateAttributes,
                sectionUpdateData = listOf(
                    UUIDConverter.fromInt(0) to SectionUpdateAttributes(duration = 15.minutes),
                    UUIDConverter.fromInt(6) to SectionUpdateAttributes(duration = 15.minutes)
                )
            )
        }

        assertThat(
            exception.message
        ).isEqualTo(
            "Section(s) with id(s) [00000000-0000-0000-0000-000000000000, 00000000-0000-0000-0000-000000000006] are not in session with id 00000000-0000-0000-0000-000000000002"
        )
    }

    @Test
    fun `Edit session with section with 0 duration, IllegalSectionException`() = runTest {
        val exception = assertThrows<InvalidSectionException> {
            editSessionUseCase(
                id = UUIDConverter.fromInt(2),
                sessionUpdateAttributes = validSessionUpdateAttributes,
                sectionUpdateData = listOf(
                    UUIDConverter.fromInt(3) to SectionUpdateAttributes(duration = 0.minutes)
                )
            )
        }

        assertThat(exception.message).isEqualTo("Section duration must be greater than 0")
    }

    @Test
    fun `Edit session with valid data, session is edited`() = runTest {
        editSessionUseCase(
            id = UUIDConverter.fromInt(2),
            sessionUpdateAttributes = validSessionUpdateAttributes,
            sectionUpdateData = validSectionUpdateData
        )

        val sessions = fakeSessionRepository.orderedSessionsWithSectionsWithLibraryItems.first()

        assertThat(sessions).containsExactly(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(2),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    breakDurationSeconds = 600,
                    rating = 5,
                    comment = "Edited comment"
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(3),
                            startTimestamp = FakeTimeProvider.START_TIME,
                            durationSeconds = 900,
                            libraryItemId = UUIDConverter.fromInt(1),
                            sessionId = UUIDConverter.fromInt(2)
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(1),
                            createdAt = FakeTimeProvider.START_TIME,
                            modifiedAt = FakeTimeProvider.START_TIME,
                            name = "Test item 1",
                            colorIndex = 5,
                            libraryFolderId = null,
                            customOrder = null
                        )
                    ),
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(4),
                            startTimestamp = FakeTimeProvider.START_TIME,
                            durationSeconds = 900,
                            libraryItemId = UUIDConverter.fromInt(1),
                            sessionId = UUIDConverter.fromInt(2)
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(1),
                            createdAt = FakeTimeProvider.START_TIME,
                            modifiedAt = FakeTimeProvider.START_TIME,
                            name = "Test item 1",
                            colorIndex = 5,
                            libraryFolderId = null,
                            customOrder = null
                        )
                    )
                ),
            )
        )
    }
}
