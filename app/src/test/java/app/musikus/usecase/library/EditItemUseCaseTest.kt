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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class EditItemUseCaseTest {
    private lateinit var editItem: EditItemUseCase
    private lateinit var fakeLibraryRepository: FakeLibraryRepository

    private lateinit var itemId: UUID
    private lateinit var folderId: UUID

    @BeforeEach
    fun setUp() {
        fakeLibraryRepository = FakeLibraryRepository()
        editItem = EditItemUseCase(fakeLibraryRepository)

        val itemCreationAttributes = LibraryItemCreationAttributes(
            name = "TestItem",
            colorIndex = 4
        )

        val folderCreationAttributes = LibraryFolderCreationAttributes("TestFolder")

        runBlocking {
            fakeLibraryRepository.addFolder(folderCreationAttributes)
            fakeLibraryRepository.addItem(itemCreationAttributes)

            itemId = fakeLibraryRepository.items.first().first().id
            folderId = fakeLibraryRepository.folders.first().first().folder.id
        }
    }


    @Test
    fun `Edit item with invalid id, InvalidLibraryItemException('Item not found')`() = runTest {
        val exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = UUID.randomUUID(),
                updateAttributes = LibraryItemUpdateAttributes()
            )
        }
        assertThat(exception.message).isEqualTo("Item not found")
    }

    @Test
    fun `Edit item with empty name, InvalidLibraryItemException('Item name cannot be empty')`() = runTest {
        val exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = itemId,
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "",
                )
            )
        }
        assertThat(exception.message).isEqualTo("Item name cannot be empty")
    }

    @Test
    fun `Edit item with invalid colorIndex, InvalidLibraryItemException('Color index must be between 0 and 9')`() = runTest {
        var exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = itemId,
                updateAttributes = LibraryItemUpdateAttributes(
                    colorIndex = -1,
                )
            )
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")

        exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = itemId,
                updateAttributes = LibraryItemUpdateAttributes(
                    colorIndex = 10,
                )
            )
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")
    }

    @Test
    fun `Edit item with non existent folderId, InvalidLibraryItemException('Folder (FOLDER_ID) does not exist')`() = runTest {
        val randomId = UUID.randomUUID()
        val exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = itemId,
                updateAttributes = LibraryItemUpdateAttributes(
                    libraryFolderId = Nullable(randomId),
                )
            )
        }
        assertThat(exception.message).isEqualTo("Folder (${randomId}) does not exist")
    }

    @Test
    fun `Edit item name, color and folderId, true`() = runTest {
        editItem(
            id = itemId,
            updateAttributes = LibraryItemUpdateAttributes(
                name = "NewName",
                colorIndex = 8,
                libraryFolderId = Nullable(folderId),
            )
        )
        val updatedItem = fakeLibraryRepository.items.first().first()

        assertThat(updatedItem.name).isEqualTo("NewName")
        assertThat(updatedItem.colorIndex).isEqualTo(8)

        val updatedFolderWithItems = fakeLibraryRepository.folders.first().first()

        assertThat(updatedFolderWithItems.items).contains(updatedItem)
    }
}