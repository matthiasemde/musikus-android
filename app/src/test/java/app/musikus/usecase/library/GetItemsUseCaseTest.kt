/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.Nullable
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.repository.FakeLibraryRepository
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.util.UUID


class GetItemsUseCaseTest {
    private lateinit var getItems: GetItemsUseCase
    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    private lateinit var folderId: UUID

    @BeforeEach
    fun setUp() {
        fakeLibraryRepository = FakeLibraryRepository()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        getItems = GetItemsUseCase(
            libraryRepository = fakeLibraryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
        )

        val itemCreationAttributes = listOf(
            "TestItem3" to 0,
            "TestItem5" to 8,
            "TestItem2" to 3,
            "TestItem1" to 9,
            "TestItem4" to 2,
        ).map { (name, colorIndex) ->
            LibraryItemCreationAttributes(
                name = name,
                libraryFolderId = Nullable(null),
                colorIndex = colorIndex
            )
        }

        val folderCreationAttributes = LibraryFolderCreationAttributes("TestFolder")

        runBlocking {
            fakeLibraryRepository.addFolder(folderCreationAttributes)
            folderId = fakeLibraryRepository.folders.first().first().folder.id

            itemCreationAttributes.forEach {
                fakeLibraryRepository.addItem(it)
                fakeLibraryRepository.addItem(it.copy(
                    libraryFolderId = Nullable(folderId)
                ))
                delay(1) // necessary to ensure that the timestamps are different
            }

            val items = fakeLibraryRepository.items.first().filter {
                it.libraryFolderId == null
            }

            val itemsInFolder = fakeLibraryRepository.items.first().filter {
                it.libraryFolderId == folderId
            }

            // rename items to mix up the 'last modified' order
            fakeLibraryRepository.editItem(
                id = items[4].id,
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "RenamedItem1",
                )
            )

            fakeLibraryRepository.editItem(
                id = itemsInFolder[4].id,
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "RenamedItem1",
                )
            )

            delay(1) // necessary to ensure that the timestamps are different

            fakeLibraryRepository.editItem(
                id = items[2].id,
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "RenamedItem2",
                )
            )

            fakeLibraryRepository.editItem(
                id = itemsInFolder[2].id,
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "RenamedItem2",
                )
            )
        }
    }

    @Test
    fun `Get items, items are sorted by 'date added' descending`() = runTest {
        // Get items
        val items = getItems(folderId = Nullable(null)).first()
        val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

        // Check if items are sorted by 'date added' descending by default
        val expectedOutcome = listOf(
            "RenamedItem1",
            "TestItem1",
            "RenamedItem2",
            "TestItem5",
            "TestItem3",
        )

        assertThat(items.map { it.name }).isEqualTo(expectedOutcome)
        assertThat(itemsInFolder.map { it.name }).isEqualTo(expectedOutcome)
    }

    @Test
    fun `Set item sort mode to 'date added' ascending then get items, items are sorted correctly`() = runTest {
        // Set sort mode to 'date added' ascending
        fakeUserPreferencesRepository.updateLibraryItemSortInfo(
            sortInfo = SortInfo(
                mode = LibraryItemSortMode.DATE_ADDED,
                direction = SortDirection.ASCENDING
            )
        )

        // Get items
        val items = getItems(folderId = Nullable(null)).first()
        val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

        // Check if items are sorted correctly
        val expectedOutcome = listOf(
            "TestItem3",
            "TestItem5",
            "RenamedItem2",
            "TestItem1",
            "RenamedItem1",
        )

        assertThat(items.map { it.name }).isEqualTo(expectedOutcome)
        assertThat(itemsInFolder.map { it.name }).isEqualTo(expectedOutcome)
    }

    @Test
    fun `Set item sort mode to 'last modified' descending then get items, items are sorted correctly`() = runTest {
        // Set sort mode to 'last modified' descending
        fakeUserPreferencesRepository.updateLibraryItemSortInfo(
            sortInfo = SortInfo(
                mode = LibraryItemSortMode.LAST_MODIFIED,
                direction = SortDirection.DESCENDING
            )
        )

        // Get items
        val items = getItems(folderId = Nullable(null)).first()
        val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

        // Check if items are sorted correctly
        val expectedOutcome = listOf(
            "RenamedItem2",
            "RenamedItem1",
            "TestItem1",
            "TestItem5",
            "TestItem3",
        )

        assertThat(items.map { it.name }).isEqualTo(expectedOutcome)
        assertThat(itemsInFolder.map { it.name }).isEqualTo(expectedOutcome)
    }

    @Test
    fun `Set item sort mode to 'last modified' ascending then get items, items are sorted correctly`() = runTest {
        // Set sort mode to 'last modified' ascending
        fakeUserPreferencesRepository.updateLibraryItemSortInfo(
            sortInfo = SortInfo(
                mode = LibraryItemSortMode.LAST_MODIFIED,
                direction = SortDirection.ASCENDING
            )
        )

        // Get items
        val items = getItems(folderId = Nullable(null)).first()
        val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

        // Check if items are sorted correctly
        val expectedOutcome = listOf(
            "TestItem3",
            "TestItem5",
            "TestItem1",
            "RenamedItem1",
            "RenamedItem2",
        )

        assertThat(items.map { it.name }).isEqualTo(expectedOutcome)
        assertThat(itemsInFolder.map { it.name }).isEqualTo(expectedOutcome)
    }

    @Test
    fun `Set item sort mode to 'name' descending then get items, items are sorted correctly`() = runTest {
        // Set sort mode to 'name' descending
        fakeUserPreferencesRepository.updateLibraryItemSortInfo(
            sortInfo = SortInfo(
                mode = LibraryItemSortMode.NAME,
                direction = SortDirection.DESCENDING
            )
        )

        // Get items
        val items = getItems(folderId = Nullable(null)).first()
        val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

        // Check if items are sorted correctly
        val expectedOutcome = listOf(
            "TestItem5",
            "TestItem3",
            "TestItem1",
            "RenamedItem2",
            "RenamedItem1",
        )

        assertThat(items.map { it.name }).isEqualTo(expectedOutcome)
        assertThat(itemsInFolder.map { it.name }).isEqualTo(expectedOutcome)
    }

    @Test
    fun `Set item sort mode to 'name' ascending then get items, items are sorted correctly`() = runTest {
        // Set sort mode to 'name' ascending
        fakeUserPreferencesRepository.updateLibraryItemSortInfo(
            sortInfo = SortInfo(
                mode = LibraryItemSortMode.NAME,
                direction = SortDirection.ASCENDING
            )
        )

        // Get items
        val items = getItems(folderId = Nullable(null)).first()
        val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

        // Check if items are sorted correctly
        val expectedOutcome = listOf(
            "RenamedItem1",
            "RenamedItem2",
            "TestItem1",
            "TestItem3",
            "TestItem5",
        )

        assertThat(items.map { it.name }).isEqualTo(expectedOutcome)
        assertThat(itemsInFolder.map { it.name }).isEqualTo(expectedOutcome)
    }

    @Test
    fun `Set item sort mode to 'color' descending then get items, items are sorted correctly`() = runTest {
        // Set sort mode to 'color' descending
        fakeUserPreferencesRepository.updateLibraryItemSortInfo(
            sortInfo = SortInfo(
                mode = LibraryItemSortMode.COLOR,
                direction = SortDirection.DESCENDING
            )
        )

        // Get items
        val items = getItems(folderId = Nullable(null)).first()
        val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

        // Check if items are sorted correctly
        val expectedOutcome = listOf(
            "TestItem1",
            "TestItem5",
            "RenamedItem2",
            "RenamedItem1",
            "TestItem3",
        )

        assertThat(items.map { it.name }).isEqualTo(expectedOutcome)
        assertThat(itemsInFolder.map { it.name }).isEqualTo(expectedOutcome)
    }

    @Test
    fun `Set item sort mode to 'color' ascending then get items, items are sorted correctly`() = runTest {
        // Set sort mode to 'color' ascending
        fakeUserPreferencesRepository.updateLibraryItemSortInfo(
            sortInfo = SortInfo(
                mode = LibraryItemSortMode.COLOR,
                direction = SortDirection.ASCENDING
            )
        )

        // Get items
        val items = getItems(folderId = Nullable(null)).first()
        val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

        // Check if items are sorted correctly
        val expectedOutcome = listOf(
            "TestItem3",
            "RenamedItem1",
            "RenamedItem2",
            "TestItem5",
            "TestItem1",
        )

        assertThat(items.map { it.name }).isEqualTo(expectedOutcome)
        assertThat(itemsInFolder.map { it.name }).isEqualTo(expectedOutcome)
    }

    @Test
    fun `Set item sort mode to 'custom' then get items, items are sorted correctly`() {
        // TODO
    }
}