/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.sessions

import app.musikus.database.Nullable
import app.musikus.database.SectionWithLibraryItem
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.InvalidSectionException
import app.musikus.database.daos.InvalidSessionException
import app.musikus.database.daos.LibraryItem
import app.musikus.database.daos.Section
import app.musikus.database.daos.Session
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.repository.FakeLibraryRepository
import app.musikus.repository.FakeSessionRepository
import app.musikus.library.domain.usecase.GetAllLibraryItemsUseCase
import app.musikus.sessionslist.domain.usecase.AddSessionUseCase
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
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