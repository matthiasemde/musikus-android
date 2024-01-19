/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.repository.FakeLibraryRepository
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import app.musikus.database.UUIDConverter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


class GetFoldersUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var getFolders: GetFoldersUseCase
    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()
        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        getFolders = GetFoldersUseCase(
            libraryRepository = fakeLibraryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
        )

        val folderCreationAttributes = listOf(
            "TestFolder3",
            "TestFolder3",
            "TestFolder2", // -> RenamedFolder2
            "TestFolder1",
            "TestFolder4", // -> RenamedFolder1
        ).map { name ->
            LibraryFolderCreationAttributes(name = name)
        }

        runBlocking {
            folderCreationAttributes.forEach {
                fakeLibraryRepository.addFolder(it)
                fakeTimeProvider.advanceTimeBy(1.seconds)
            }

            // rename folders to mix up the 'last modified' order
            fakeLibraryRepository.editFolder(
                id = UUIDConverter.fromInt(5),
                updateAttributes = LibraryFolderUpdateAttributes(
                    name = "RenamedFolder1"
                )
            )

            fakeTimeProvider.advanceTimeBy(1.seconds)

            fakeLibraryRepository.editFolder(
                id = UUIDConverter.fromInt(3),
                updateAttributes = LibraryFolderUpdateAttributes(
                    name = "RenamedFolder2"
                )
            )
        }
    }

    @Test
    fun `Get folders, list contains all folders`() = runTest {
        val folders = getFolders().first()

        assertThat(folders).containsExactly(
            LibraryFolderWithItems(
                folder = LibraryFolder(
                    id = UUIDConverter.fromInt(1),
                    createdAt = FakeTimeProvider.START_TIME.plus(0.seconds.toJavaDuration()),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(0.seconds.toJavaDuration()),
                    name = "TestFolder3",
                    customOrder = null
                ),
                items = emptyList()
            ),
            LibraryFolderWithItems(
                folder = LibraryFolder(
                    id = UUIDConverter.fromInt(2),
                    createdAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
                    name = "TestFolder3",
                    customOrder = null
                ),
                items = emptyList()
            ),
            LibraryFolderWithItems(
                folder = LibraryFolder(
                    id = UUIDConverter.fromInt(3),
                    createdAt = FakeTimeProvider.START_TIME.plus(2.seconds.toJavaDuration()),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(6.seconds.toJavaDuration()),
                    name = "RenamedFolder2",
                    customOrder = null
                ),
                items = emptyList()
            ),
            LibraryFolderWithItems(
                folder = LibraryFolder(
                    id = UUIDConverter.fromInt(4),
                    createdAt = FakeTimeProvider.START_TIME.plus(3.seconds.toJavaDuration()),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(3.seconds.toJavaDuration()),
                    name = "TestFolder1",
                    customOrder = null
                ),
                items = emptyList()
            ),
            LibraryFolderWithItems(
                folder = LibraryFolder(
                    id = UUIDConverter.fromInt(5),
                    createdAt = FakeTimeProvider.START_TIME.plus(4.seconds.toJavaDuration()),
                    modifiedAt = FakeTimeProvider.START_TIME.plus(5.seconds.toJavaDuration()),
                    name = "RenamedFolder1",
                    customOrder = null
                ),
                items = emptyList()
            )
        )
    }

    @Test
    fun `Get folders, folders are sorted by 'date added' descending`() = runTest {
        val folderIds = getFolders().first().map { it.folder.id }

        val expectedFolderIds = listOf(5, 4, 3, 2, 1).map { UUIDConverter.fromInt(it) }

        assertThat(folderIds).isEqualTo(expectedFolderIds)
    }

    @Test
    fun `Set folder sort mode to 'date added' ascending then get folders, folders are sorted correctly`() = runTest {
        // Set sort mode to 'date added' ascending
        fakeUserPreferencesRepository.updateLibraryFolderSortInfo(
            SortInfo(
                mode = LibraryFolderSortMode.DATE_ADDED,
                direction = SortDirection.ASCENDING
            )
        )

        val expectedFolderIds = listOf(1, 2, 3, 4, 5).map { UUIDConverter.fromInt(it) }

        // Get folders and map them to their id and map them to their id
        val folderIs = getFolders().first().map { it.folder.id }

        // Check if folders are sorted correctly
        assertThat(folderIs).isEqualTo(expectedFolderIds)
    }

    @Test
    fun `Set folder sort mode to 'last modified' descending then get folders, folders are sorted correctly`() = runTest {
        // Set sort mode to 'last modified' descending
        fakeUserPreferencesRepository.updateLibraryFolderSortInfo(
            SortInfo(
                mode = LibraryFolderSortMode.LAST_MODIFIED,
                direction = SortDirection.DESCENDING
            )
        )

        val expectedFolderIds = listOf(3, 5, 4, 2, 1).map { UUIDConverter.fromInt(it) }

        // Get folders and map them to their id
        val folderIds = getFolders().first().map { it.folder.id }

        // Check if folders are sorted correctly
        assertThat(folderIds).isEqualTo(expectedFolderIds)
    }

    @Test
    fun `Set folder sort mode to 'last modified' ascending then get folders, folders are sorted correctly`() = runTest {
        // Set sort mode to 'last modified' ascending
        fakeUserPreferencesRepository.updateLibraryFolderSortInfo(
            SortInfo(
                mode = LibraryFolderSortMode.LAST_MODIFIED,
                direction = SortDirection.ASCENDING
            )
        )

        val expectedFolderIds = listOf(1, 2, 4, 5, 3).map { UUIDConverter.fromInt(it) }

        // Get folders and map them to their id
        val folderIds = getFolders().first().map { it.folder.id }

        // Check if folders are sorted correctly
        assertThat(folderIds).isEqualTo(expectedFolderIds)
    }

    @Test
    fun `Set folder sort mode to 'name' descending then get folders, folders are sorted correctly`() = runTest {
        // Set sort mode to 'name' descending
        fakeUserPreferencesRepository.updateLibraryFolderSortInfo(
            SortInfo(
                mode = LibraryFolderSortMode.NAME,
                direction = SortDirection.DESCENDING
            )
        )

        val expectedFolderIds = listOf(
            1, // TestFolder3
            2, // TestFolder3
            4, // TestFolder1
            3, // RenamedFolder2
            5, // RenamedFolder1
        ).map { UUIDConverter.fromInt(it) }

        // Get folders and map them to their id
        val folderIds = getFolders().first().map { it.folder.id }

        // Check if folders are sorted correctly
        assertThat(folderIds).isEqualTo(expectedFolderIds)
    }

    @Test
    fun `Set folder sort mode to 'name' ascending then get folders, folders are sorted correctly`() = runTest {
        // Set sort mode to 'name' ascending
        fakeUserPreferencesRepository.updateLibraryFolderSortInfo(
            SortInfo(
                mode = LibraryFolderSortMode.NAME,
                direction = SortDirection.ASCENDING
            )
        )

        val expectedOutcome = listOf(
            5, // RenamedFolder1
            3, // RenamedFolder2
            4, // TestFolder1
            1, // TestFolder3
            2, // TestFolder3
        ).map { UUIDConverter.fromInt(it) }

        // Get folders and map them to their id
        val folderIds = getFolders().first().map { it.folder.id }

        // Check if folders are sorted correctly
        assertThat(folderIds).isEqualTo(expectedOutcome)
    }
}