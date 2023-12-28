/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.daos.InvalidLibraryFolderException
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.repository.FakeLibraryRepository
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import app.musikus.utils.intToUUID
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

    private lateinit var editFolder: EditFolderUseCase
    private lateinit var fakeLibraryRepository: FakeLibraryRepository

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()
        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
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
                id = intToUUID(1),
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
            id = intToUUID(1),
            updateAttributes = LibraryFolderUpdateAttributes(
                name = "NewName",
            )
        )

        val updatedFolder = fakeLibraryRepository.folders.first().first().folder

        assertThat(updatedFolder).isEqualTo(LibraryFolder(
            name = "NewName",
            customOrder = null
        ).apply {
            setId(intToUUID(1))
            setCreatedAt(fakeTimeProvider.startTime)
            setModifiedAt(fakeTimeProvider.startTime)
        })
    }
}