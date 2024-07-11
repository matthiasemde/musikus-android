/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.library.domain.usecase.GetSortedLibraryItemsUseCase
import app.musikus.repository.FakeLibraryRepository
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.settings.domain.usecase.GetItemSortInfoUseCase
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


class GetSortedLibraryItemsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    /** SUT */
    private lateinit var getSortedItems: GetSortedLibraryItemsUseCase

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()
        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()

        /** SUT */
        getSortedItems = GetSortedLibraryItemsUseCase(
            libraryRepository = fakeLibraryRepository,
            getItemSortInfo = GetItemSortInfoUseCase(fakeUserPreferencesRepository),
        )

        val itemCreationAttributes = listOf(
            "TestItem3" to 0,
            "TestItem3" to 8,
            "TestItem2" to 3, // -> RenamedItem2
            "TestItem1" to 9,
            "TestItem4" to 2, // -> RenamedItem1
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

            itemCreationAttributes.forEach {
                fakeLibraryRepository.addItem(it)
                fakeLibraryRepository.addItem(it.copy(
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1))
                ))
                fakeTimeProvider.advanceTimeBy(1.seconds) // necessary to ensure that the timestamps are different
            }

            // rename items to mix up the 'last modified' order
            fakeLibraryRepository.editItem(
                id = UUIDConverter.fromInt(10),
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "RenamedItem1",
                )
            )

            fakeLibraryRepository.editItem(
                id = UUIDConverter.fromInt(11),
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "RenamedItem1",
                )
            )

            fakeTimeProvider.advanceTimeBy(1.seconds) // necessary to ensure that the timestamps are different

            fakeLibraryRepository.editItem(
                id = UUIDConverter.fromInt(6),
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "RenamedItem2",
                )
            )

            fakeLibraryRepository.editItem(
                id = UUIDConverter.fromInt(7),
                updateAttributes = LibraryItemUpdateAttributes(
                    name = "RenamedItem2",
                )
            )
        }
    }

    @Test
    fun `Get items from root folder, list contains all items`() = runTest {
        val items = getSortedItems(folderId = Nullable(null)).first()

        assertThat(items).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(2),
                createdAt = FakeTimeProvider.START_TIME.plus(0.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(0.seconds.toJavaDuration()),
                name = "TestItem3",
                colorIndex = 0,
                libraryFolderId = null,
                customOrder = null
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(4),
                createdAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
                name = "TestItem3",
                colorIndex = 8,
                libraryFolderId = null,
                customOrder = null
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(6),
                createdAt = FakeTimeProvider.START_TIME.plus(2.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(6.seconds.toJavaDuration()),
                name = "RenamedItem2",
                colorIndex = 3,
                libraryFolderId = null,
                customOrder = null
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(8),
                createdAt = FakeTimeProvider.START_TIME.plus(3.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(3.seconds.toJavaDuration()),
                name = "TestItem1",
                colorIndex = 9,
                libraryFolderId = null,
                customOrder = null
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(10),
                createdAt = FakeTimeProvider.START_TIME.plus(4.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(5.seconds.toJavaDuration()),
                name = "RenamedItem1",
                colorIndex = 2,
                libraryFolderId = null,
                customOrder = null
            ),
        )
    }

    @Test
    fun `Get items from 'TestFolder', list contains all items`() = runTest {
        val items = getSortedItems(folderId = Nullable(UUIDConverter.fromInt(1))).first()

        assertThat(items).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                createdAt = FakeTimeProvider.START_TIME.plus(0.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(0.seconds.toJavaDuration()),
                name = "TestItem3",
                colorIndex = 0,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(5),
                createdAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
                name = "TestItem3",
                colorIndex = 8,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(7),
                createdAt = FakeTimeProvider.START_TIME.plus(2.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(6.seconds.toJavaDuration()),
                name = "RenamedItem2",
                colorIndex = 3,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(9),
                createdAt = FakeTimeProvider.START_TIME.plus(3.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(3.seconds.toJavaDuration()),
                name = "TestItem1",
                colorIndex = 9,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(11),
                createdAt = FakeTimeProvider.START_TIME.plus(4.seconds.toJavaDuration()),
                modifiedAt = FakeTimeProvider.START_TIME.plus(5.seconds.toJavaDuration()),
                name = "RenamedItem1",
                colorIndex = 2,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null
            )
        )
    }

    @Test
    fun `Get items, items are sorted by 'date added' descending`() = runTest {
        // Get items and map them to their ids
        val itemIds = getSortedItems(folderId = Nullable(null)).first().map { it.id }

        // Check if items are sorted by 'date added' descending by default
        val expectedItemIds = listOf(10, 8, 6, 4, 2).map { UUIDConverter.fromInt(it) }

        assertThat(itemIds).isEqualTo(expectedItemIds)
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

        // Get items and map them to their ids
        val itemIds = getSortedItems(folderId = Nullable(null)).first().map { it.id }

        // Check if items are sorted correctly
        val expectedItemIds = listOf(2, 4, 6, 8, 10).map { UUIDConverter.fromInt(it) }

        assertThat(itemIds).isEqualTo(expectedItemIds)
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

        // Get items and map them to their ids
        val itemIds = getSortedItems(folderId = Nullable(null)).first().map { it.id }

        // Check if items are sorted correctly
        val expectedItemIds = listOf(6, 10, 8, 4, 2).map { UUIDConverter.fromInt(it) }

        assertThat(itemIds).isEqualTo(expectedItemIds)
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

        // Get items and map them to their ids
        val itemIds = getSortedItems(folderId = Nullable(null)).first().map { it.id }

        // Check if items are sorted correctly
        val expectedItemIds = listOf(2, 4, 8, 10, 6).map { UUIDConverter.fromInt(it) }

        assertThat(itemIds).isEqualTo(expectedItemIds)
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

        // Get items and map them to their ids
        val itemIds = getSortedItems(folderId = Nullable(null)).first().map { it.id }

        // Check if items are sorted correctly
        val expectedItemIds = listOf(
            2, // TestItem3
            4, // TestItem3
            8, // TestItem1
            6, // RenamedItem2
            10, // RenamedItem1
        ).map { UUIDConverter.fromInt(it) }

        assertThat(itemIds).isEqualTo(expectedItemIds)
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

        // Get items and map them to their ids
        val itemIds = getSortedItems(folderId = Nullable(null)).first().map { it.id }

        // Check if items are sorted correctly
        val expectedItemIds = listOf(
            10, // RenamedItem1
            6, // RenamedItem2
            8, // TestItem1
            2, // TestItem3
            4, // TestItem3
        ).map { UUIDConverter.fromInt(it) }

        assertThat(itemIds).isEqualTo(expectedItemIds)
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

        // Get items and map them to their ids
        val itemIds = getSortedItems(folderId = Nullable(null)).first().map { it.id }

        // Check if items are sorted correctly
        val expectedItemIds = listOf(
            8, // TestItem1 -> colorIndex: 9
            4, // TestItem3 -> colorIndex: 8
            6, // RenamedItem2 -> colorIndex: 3
            10, // RenamedItem1 -> colorIndex: 2
            2, // TestItem3 -> colorIndex: 0
        ).map { UUIDConverter.fromInt(it) }

        assertThat(itemIds).isEqualTo(expectedItemIds)
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

        // Get items and map them to their ids
        val itemIds = getSortedItems(folderId = Nullable(null)).first().map { it.id }

        // Check if items are sorted correctly
        val expectedItemIds = listOf(
            2, // TestItem3 -> colorIndex: 0
            10, // RenamedItem1 -> colorIndex: 2
            6, // RenamedItem2 -> colorIndex: 3
            4, // TestItem3 -> colorIndex: 8
            8, // TestItem1 -> colorIndex: 9
        ).map { UUIDConverter.fromInt(it) }

        assertThat(itemIds).isEqualTo(expectedItemIds)
    }

    @Test
    fun `Set item sort mode to 'custom' then get items, items are sorted correctly`() {
        // TODO
    }
}