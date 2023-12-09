/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.Nullable
import app.musikus.database.entities.InvalidLibraryItemException
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.repository.FakeLibraryRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class EditItemUseCaseTest {
    private lateinit var  editItem: EditItemUseCase
    private lateinit var fakeRepository: FakeLibraryRepository
    @BeforeEach
    fun setUp() {
        fakeRepository = FakeLibraryRepository()
        editItem = EditItemUseCase(fakeRepository)

        val itemCreationAttributes = ('a'..'z').mapIndexed { index, name ->
            LibraryItemCreationAttributes(
                name = name.toString(),
                libraryFolderId = Nullable(null),
                colorIndex = index % 10
            )
        }

        val folderCreationAttributes = ('a'..'z').map { name ->
            LibraryFolderCreationAttributes(
                name = name.toString(),
            )
        }

        runBlocking {
            folderCreationAttributes.shuffled().forEach {
                fakeRepository.addFolder(it)
            }
            itemCreationAttributes.shuffled().forEach {
                fakeRepository.addItem(it)
            }
        }
    }


    @Test
    fun `Edit item with empty name, InvalidLibraryItemException('Item name cannot be empty')`() {
        val exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                val item = fakeRepository.items.first().first()
                editItem(
                    id = item.id,
                    updateAttributes = LibraryItemUpdateAttributes(
                        name = "",
                    )
                )
            }
        }
        assertThat(exception.message).isEqualTo("Item name cannot be empty")
    }

    @Test
    fun `Edit item with invalid colorIndex, InvalidLibraryItemException('Color index must be between 0 and 9')`() {
        var exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                val item = fakeRepository.items.first().random()
                editItem(
                    id = item.id,
                    updateAttributes = LibraryItemUpdateAttributes(
                        colorIndex = -1,
                    )
                )
            }
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")

        exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                val item = fakeRepository.items.first().random()
                editItem(
                    id = item.id,
                    updateAttributes = LibraryItemUpdateAttributes(
                        colorIndex = 10,
                    )
                )
            }
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")
    }

    @Test
    fun `Edit item with non existent folderId, InvalidLibraryItemException('Folder (FOLDER_ID) does not exist')`() {
        val randomId = UUID.randomUUID()
        val exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                val item = fakeRepository.items.first().random()
                editItem(
                    id = item.id,
                    updateAttributes = LibraryItemUpdateAttributes(
                        libraryFolderId = Nullable(randomId),
                    )
                )
            }
        }
        assertThat(exception.message).isEqualTo("Folder (${randomId}) does not exist")
    }

    @Test
    fun `Edit item name, color and folderId, true`() {
        runBlocking {
            val folder = fakeRepository.folders.first().random().folder
            val item = fakeRepository.items.first().random()
            editItem(
                id = item.id,
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "test",
                    colorIndex = (item.colorIndex + 5) % 10,
                    libraryFolderId = Nullable(folder.id),
                )
            )
            val updatedItem = fakeRepository.items.first().first { it.id == item.id }
            val updatedFolder = fakeRepository.folders.first().first { it.folder.id == folder.id }

            assertThat(updatedItem.name).isEqualTo("test")
            assertThat(updatedItem.colorIndex).isEqualTo((item.colorIndex + 5) % 10)
            assertThat(updatedItem.libraryFolderId).isEqualTo(folder.id)

            assertThat(updatedFolder.items).contains(updatedItem)
        }
    }
}