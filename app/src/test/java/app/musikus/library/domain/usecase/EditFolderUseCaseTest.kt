/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.library.data.daos.InvalidLibraryFolderException
import app.musikus.library.data.daos.LibraryFolder
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryFolderUpdateAttributes
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.core.data.UUIDConverter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID


class EditFolderUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository

    /** SUT */
    private lateinit var editFolder: EditFolderUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()
        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)

        /** SUT */
        editFolder = EditFolderUseCase(fakeLibraryRepository)

        val folderCreationAttributes = LibraryFolderCreationAttributes("TestFolder")

        runBlocking {
            fakeLibraryRepository.addFolder(folderCreationAttributes)
        }
    }

    @Test
    fun `Edit folder with invalid id, InvalidLibraryFolderException('Folder not found')`() = runTest {
        val exception = assertThrows<InvalidLibraryFolderException> {
            editFolder(
                id = UUID.randomUUID(),
                updateAttributes = LibraryFolderUpdateAttributes()
            )
        }

        assertThat(exception.message).isEqualTo("Folder not found")
    }

    @Test
    fun `Edit folder with empty name, InvalidLibraryFolderException('Folder name can not be empty')`() = runTest {
        val exception = assertThrows<InvalidLibraryFolderException> {
            editFolder(
                id = UUIDConverter.fromInt(1),
                updateAttributes = LibraryFolderUpdateAttributes(
                    name = "",
                )
            )
        }

        assertThat(exception.message).isEqualTo("Folder name can not be empty")
    }

    @Test
    fun `Edit folder with valid name, folder name is updated`() = runTest {
        editFolder(
            id = UUIDConverter.fromInt(1),
            updateAttributes = LibraryFolderUpdateAttributes(
                name = "NewName",
            )
        )

        val updatedFolder = fakeLibraryRepository.folders.first().first().folder

        assertThat(updatedFolder).isEqualTo(
            LibraryFolder(
            id = UUIDConverter.fromInt(1),
            createdAt = FakeTimeProvider.START_TIME,
            modifiedAt = FakeTimeProvider.START_TIME,
            name = "NewName",
            customOrder = null
        )
        )
    }
}