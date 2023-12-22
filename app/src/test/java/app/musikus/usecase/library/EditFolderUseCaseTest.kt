/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.daos.InvalidLibraryFolderException
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.repository.FakeLibraryRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID


class EditFolderUseCaseTest {
    private lateinit var editFolder: EditFolderUseCase
    private lateinit var fakeLibraryRepository: FakeLibraryRepository

    private lateinit var folderId: UUID

    @BeforeEach
    fun setUp() {
        fakeLibraryRepository = FakeLibraryRepository()
        editFolder = EditFolderUseCase(fakeLibraryRepository)

        val folderCreationAttributes = LibraryFolderCreationAttributes("TestFolder")

        runBlocking {
            fakeLibraryRepository.addFolder(folderCreationAttributes)
            folderId = fakeLibraryRepository.folders.first().first().folder.id
        }
    }

    @Test
    fun `Edit folder with invalid id, InvalidLibraryFolderException('Folder not found')`() {
        val exception = assertThrows<InvalidLibraryFolderException> {
            runBlocking {
                editFolder(
                    id = UUID.randomUUID(),
                    updateAttributes = LibraryFolderUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo("Folder not found")
    }

    @Test
    fun `Edit folder with empty name, InvalidLibraryFolderException('Folder name can not be empty')`() {
        val exception = assertThrows<InvalidLibraryFolderException> {
            runBlocking {
                editFolder(
                    id = folderId,
                    updateAttributes = LibraryFolderUpdateAttributes(
                        name = "",
                    )
                )
            }
        }

        assertThat(exception.message).isEqualTo("Folder name can not be empty")
    }

    @Test
    fun `Edit folder with valid name, folder name is updated`() {
        runBlocking {
            editFolder(
                id = folderId,
                updateAttributes = LibraryFolderUpdateAttributes(
                    name = "NewName",
                )
            )

            val updatedFolder = fakeLibraryRepository.folders.first().first().folder

            assertThat(updatedFolder.name).isEqualTo("NewName")
        }
    }
}