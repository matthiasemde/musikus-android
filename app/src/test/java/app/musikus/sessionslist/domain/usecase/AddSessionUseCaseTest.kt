/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessionslist.domain.usecase

import app.musikus.core.data.Nullable
import app.musikus.core.data.SectionWithLibraryItem
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.sessionslist.data.daos.InvalidSectionException
import app.musikus.sessionslist.data.daos.InvalidSessionException
import app.musikus.library.data.daos.LibraryItem
import app.musikus.sessionslist.data.daos.Section
import app.musikus.sessionslist.data.daos.Session
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.sessionslist.data.entities.SectionCreationAttributes
import app.musikus.sessionslist.data.entities.SessionCreationAttributes
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.sessionslist.data.FakeSessionRepository
import app.musikus.library.domain.usecase.GetAllLibraryItemsUseCase
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.minutes

class AddSessionUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeSessionRepository: FakeSessionRepository

    /** SUT */
    private lateinit var addSession: AddSessionUseCase

    private val validSessionCreationAttributes = SessionCreationAttributes(
        rating = 3,
        breakDuration = 10.minutes,
        comment = "Test comment"
    )

    private val validSectionCreationAttributes by lazy { (1..2).map { index ->
        SectionCreationAttributes(
            libraryItemId = UUIDConverter.fromInt(index),
            startTimestamp = fakeTimeProvider.now(),
            duration = 10.minutes
        )
    }}

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeSessionRepository = FakeSessionRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)

        /** SUT */
        addSession = AddSessionUseCase(
            fakeSessionRepository,
            GetAllLibraryItemsUseCase(fakeLibraryRepository)
        )

        runBlocking {
            repeat(2) { index ->
                fakeLibraryRepository.addItem(
                    LibraryItemCreationAttributes(
                        name = "Test item ${index + 1}",
                        colorIndex = 5,
                        libraryFolderId = Nullable(null)
                    )
                )
            }
        }
    }

    @Test
    fun `Add session with invalid rating, InvalidSessionException`() = runTest {
        val exception = assertThrows<InvalidSessionException> {
            addSession(
                validSessionCreationAttributes.copy(rating = 0),
                validSectionCreationAttributes
            )
        }

        assertThat(exception.message).isEqualTo("Rating must be between 1 and 5")
    }

    @Test
    fun `Add session with invalid comment, InvalidSessionException`() = runTest {
        val exception = assertThrows<InvalidSessionException> {
            addSession(
                validSessionCreationAttributes.copy(comment = "a".repeat(501)),
                validSectionCreationAttributes
            )
        }

        assertThat(exception.message).isEqualTo("Comment must be less than 500 characters")
    }

    @Test
    fun `Add session with invalid break duration, InvalidSessionException`() = runTest {
        val exception = assertThrows<InvalidSessionException> {
            addSession(
                validSessionCreationAttributes.copy(breakDuration = (-1).minutes),
                validSectionCreationAttributes
            )
        }

        assertThat(exception.message).isEqualTo("Break duration must be greater than or equal to 0")
    }

    @Test
    fun `Add session with no sections, InvalidSectionException`() = runTest {
        val exception = assertThrows<InvalidSectionException> {
            addSession(
                validSessionCreationAttributes,
                emptyList()
            )
        }

        assertThat(exception.message).isEqualTo("Each session must include at least one section")
    }

    @Test
    fun `Add session with sections with invalid duration, InvalidSectionException`() = runTest {
        val exception = assertThrows<InvalidSectionException> {
            addSession(
                validSessionCreationAttributes,
                validSectionCreationAttributes.map { it.copy(duration = 0.minutes) }
            )
        }

        assertThat(exception.message).isEqualTo("Section duration must be greater than 0")
    }

    @Test
    fun `Add session with sections with set session id, InvalidSectionException`() = runTest {
        val exception = assertThrows<InvalidSectionException> {
            addSession(
                validSessionCreationAttributes,
                validSectionCreationAttributes.map { it.copy(sessionId = UUIDConverter.fromInt(1)) }
            )
        }

        assertThat(exception.message).isEqualTo("Session id must not be set, it is set automatically")
    }

    @Test
    fun `Add session with sections with non-existent library item, InvalidSectionException`() = runTest {
        val exception = assertThrows<InvalidSectionException> {
            addSession(
                validSessionCreationAttributes,
                validSectionCreationAttributes.map { it.copy(libraryItemId = UUIDConverter.fromInt(0)) }
            )
        }

        assertThat(exception.message).isEqualTo("Library items do not exist: [00000000-0000-0000-0000-000000000000]")
    }

    @Test
    fun `Add valid session, session is added`() = runTest {
        addSession(
            validSessionCreationAttributes,
            validSectionCreationAttributes
        )

        val sessions = fakeSessionRepository.orderedSessionsWithSectionsWithLibraryItems.first()

        assertThat(sessions).containsExactly(
            SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.fromInt(3),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    breakDurationSeconds = 600,
                    rating = 3,
                    comment = "Test comment"
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.fromInt(4),
                            startTimestamp = FakeTimeProvider.START_TIME,
                            durationSeconds = 600,
                            libraryItemId = UUIDConverter.fromInt(1),
                            sessionId = UUIDConverter.fromInt(3)
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
                            id = UUIDConverter.fromInt(5),
                            startTimestamp = FakeTimeProvider.START_TIME,
                            durationSeconds = 600,
                            libraryItemId = UUIDConverter.fromInt(2),
                            sessionId = UUIDConverter.fromInt(3)
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.fromInt(2),
                            createdAt = FakeTimeProvider.START_TIME,
                            modifiedAt = FakeTimeProvider.START_TIME,
                            name = "Test item 2",
                            colorIndex = 5,
                            libraryFolderId = null,
                            customOrder = null
                        )
                    ),
                )
            )
        )
    }
}