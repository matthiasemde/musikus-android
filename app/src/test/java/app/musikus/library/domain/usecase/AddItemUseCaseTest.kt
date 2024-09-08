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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AddItemUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository

    /** SUT */
    private lateinit var addItem: AddItemUseCase

    private val validItemCreationAttributes = LibraryItemCreationAttributes(
        name = "test",
        libraryFolderId = Nullable(null),
        colorIndex = 0
    )

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()
        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)

        /** SUT */
        addItem = AddItemUseCase(fakeLibraryRepository)

        val folderCreationAttributes = LibraryFolderCreationAttributes(
            name = "test",
        )

        runBlocking {
            fakeLibraryRepository.addFolder(folderCreationAttributes)
        }
    }

    @Test
    fun `Add item with empty name, InvalidLibraryItemException('Item name cannot be empty')`() = runTest {
        val exception = assertThrows<InvalidLibraryItemException> {
            addItem(validItemCreationAttributes.copy(name = ""))
        }
        assertThat(exception.message).isEqualTo("Item name cannot be empty")
    }

    @Test
    fun `Add item with invalid colorIndex, InvalidLibraryItemException('Color index must be between 0 and 9')`() = runTest {
        var exception = assertThrows<InvalidLibraryItemException> {
            addItem(validItemCreationAttributes.copy(colorIndex = -1))
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")

        exception = assertThrows<InvalidLibraryItemException> {
            addItem(validItemCreationAttributes.copy(colorIndex = 10))
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")
    }

    @Test
    fun `Add item with non existent folderId, InvalidLibraryItemException('Folder (FOLDER_ID) does not exist')`() = runTest {
        val randomId = UUID.randomUUID()
        val exception = assertThrows<InvalidLibraryItemException> {
            addItem(validItemCreationAttributes.copy(libraryFolderId = Nullable(randomId)))
        }
        assertThat(exception.message).isEqualTo("Folder ($randomId) does not exist")
    }

    @Test
    fun `Add valid item to root, item is added to root`() = runTest {
        addItem(validItemCreationAttributes.copy(libraryFolderId = Nullable(null)))

        val addedItem = fakeLibraryRepository.items.first().first()

        assertThat(addedItem).isEqualTo(
            LibraryItem(
                id = UUIDConverter.fromInt(2), // id 1 is already used by the folder
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME,
                name = validItemCreationAttributes.name,
                colorIndex = validItemCreationAttributes.colorIndex,
                customOrder = null,
                libraryFolderId = null,
            )
        )
    }

    @Test
    fun `Add valid item to folder, item is added to folder`() = runTest {
        addItem(validItemCreationAttributes.copy(libraryFolderId = Nullable(UUIDConverter.fromInt(1))))

        val addedItem = fakeLibraryRepository.items.first().first()

        val expectedItem = LibraryItem(
            id = UUIDConverter.fromInt(2), // id 1 is already used by the folder
            createdAt = FakeTimeProvider.START_TIME,
            modifiedAt = FakeTimeProvider.START_TIME,
            name = validItemCreationAttributes.name,
            colorIndex = validItemCreationAttributes.colorIndex,
            customOrder = null,
            libraryFolderId = UUIDConverter.fromInt(1),
        )

        assertThat(addedItem).isEqualTo(expectedItem)

        val folderItems = fakeLibraryRepository.folders.first().first().items

        assertThat(folderItems).containsExactly(expectedItem)
    }
}
