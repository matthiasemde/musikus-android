/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.library.domain.usecase

import app.musikus.core.data.Nullable
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.FakeIdProvider
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.library.data.FakeLibraryRepository
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.entities.InvalidLibraryItemException
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.library.data.entities.LibraryItemUpdateAttributes
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EditItemUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository

    /** SUT */
    private lateinit var editItem: EditItemUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()
        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)

        /** SUT */
        editItem = EditItemUseCase(fakeLibraryRepository)

        val itemCreationAttributes = LibraryItemCreationAttributes(
            name = "TestItem",
            colorIndex = 4
        )

        val folderCreationAttributes = LibraryFolderCreationAttributes("TestFolder")

        runBlocking {
            fakeLibraryRepository.addFolder(folderCreationAttributes)
            fakeLibraryRepository.addItem(itemCreationAttributes)
        }
    }

    @Test
    fun `Edit item with invalid id, InvalidLibraryItemException('Item not found')`() = runTest {
        val exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = UUIDConverter.fromInt(0),
                updateAttributes = LibraryItemUpdateAttributes()
            )
        }
        assertThat(exception.message).isEqualTo("Item not found")
    }

    @Test
    fun `Edit item with empty name, InvalidLibraryItemException('Item name cannot be empty')`() = runTest {
        val exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = UUIDConverter.fromInt(2),
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
                id = UUIDConverter.fromInt(2),
                updateAttributes = LibraryItemUpdateAttributes(
                    colorIndex = -1,
                )
            )
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")

        exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = UUIDConverter.fromInt(2),
                updateAttributes = LibraryItemUpdateAttributes(
                    colorIndex = 10,
                )
            )
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")
    }

    @Test
    fun `Edit item with non existent folderId, InvalidLibraryItemException('Folder (FOLDER_ID) does not exist')`() = runTest {
        val nonExistentFolderId = UUIDConverter.fromInt(0)
        val exception = assertThrows<InvalidLibraryItemException> {
            editItem(
                id = UUIDConverter.fromInt(2),
                updateAttributes = LibraryItemUpdateAttributes(
                    libraryFolderId = Nullable(nonExistentFolderId),
                )
            )
        }
        assertThat(exception.message).isEqualTo("Folder ($nonExistentFolderId) does not exist")
    }

    @Test
    fun `Edit item name, color and folderId, item is updated`() = runTest {
        editItem(
            id = UUIDConverter.fromInt(2),
            updateAttributes = LibraryItemUpdateAttributes(
                name = "NewName",
                colorIndex = 8,
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
            )
        )

        val updatedItem = fakeLibraryRepository.items.first().first()

        val expectedItem = LibraryItem(
            id = UUIDConverter.fromInt(2),
            createdAt = FakeTimeProvider.START_TIME,
            modifiedAt = FakeTimeProvider.START_TIME,
            name = "NewName",
            colorIndex = 8,
            customOrder = null,
            libraryFolderId = UUIDConverter.fromInt(1),
        )

        assertThat(updatedItem).isEqualTo(expectedItem)

        val updatedFolderItems = fakeLibraryRepository.folders.first().first().items

        assertThat(updatedFolderItems).containsExactly(expectedItem)
    }
}
