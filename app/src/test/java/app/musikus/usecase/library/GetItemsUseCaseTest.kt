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
            }
        }
    }

    private fun testItemSorting(
        sortMode: LibraryItemSortMode,
        sortDirection: SortDirection,
    ) {
        runBlocking {
            fakeUserPreferencesRepository.updateLibraryItemSortInfo(
                sortInfo = SortInfo(
                    mode = sortMode,
                    direction = sortDirection
                )
            )

            val items = getItems(folderId = Nullable(null)).first()
            val itemsInFolder = getItems(folderId = Nullable(folderId)).first()

            when (sortDirection) {
                SortDirection.ASCENDING -> {
                    assertThat(items).isInOrder(sortMode.comparator)
                    assertThat(itemsInFolder).isInOrder(sortMode.comparator)
                }
                SortDirection.DESCENDING -> {
                    assertThat(items).isInOrder(sortMode.comparator.reversed())
                    assertThat(itemsInFolder).isInOrder(sortMode.comparator.reversed())
                }
            }
        }
    }

    @Test
    fun `Get items, items are sorted by 'date added' descending`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.DATE_ADDED,
            sortDirection = SortDirection.DESCENDING
        )
    }

    @Test
    fun `Set item sort mode to 'date added' ascending then get items, items are sorted correctly`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.DATE_ADDED,
            sortDirection = SortDirection.ASCENDING
        )
    }

    @Test
    fun `Set item sort mode to 'name' descending then get items, items are sorted correctly`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.NAME,
            sortDirection = SortDirection.DESCENDING
        )
    }

    @Test
    fun `Set item sort mode to 'name' ascending then get items, items are sorted correctly`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.NAME,
            sortDirection = SortDirection.ASCENDING
        )
    }

    @Test
    fun `Set item sort mode to 'last modified' descending then get items, items are sorted correctly`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.LAST_MODIFIED,
            sortDirection = SortDirection.DESCENDING
        )
    }

    @Test
    fun `Set item sort mode to 'last modified' ascending then get items, items are sorted correctly`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.LAST_MODIFIED,
            sortDirection = SortDirection.ASCENDING
        )
    }

    @Test
    fun `Set item sort mode to 'color' descending then get items, items are sorted correctly`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.COLOR,
            sortDirection = SortDirection.DESCENDING
        )
    }

    @Test
    fun `Set item sort mode to 'color' ascending then get items, items are sorted correctly`() {
        testItemSorting(
            sortMode = LibraryItemSortMode.COLOR,
            sortDirection = SortDirection.ASCENDING
        )
    }

    @Test
    fun `Set item sort mode to 'custom' then get items, items are sorted correctly`() {
        // TODO
    }
}