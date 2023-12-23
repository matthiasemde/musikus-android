/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.repository.FakeLibraryRepository
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import app.musikus.utils.getCurrentDateTime
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class GetFoldersUseCaseTest {
    private lateinit var getFolders: GetFoldersUseCase
    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @BeforeEach
    fun setUp() {
        fakeLibraryRepository = FakeLibraryRepository()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        getFolders = GetFoldersUseCase(
            libraryRepository = fakeLibraryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
        )

        val folderCreationAttributes = listOf(
            "TestFolder3",
            "TestFolder5",
            "TestFolder2",
            "TestFolder1",
            "TestFolder4",
        ).map { name ->
            LibraryFolderCreationAttributes(name = name)
        }

        runBlocking {
            folderCreationAttributes.forEach {
                fakeLibraryRepository.addFolder(it)
                println("${getCurrentDateTime()}")
                delay(1) // necessary to ensure that the timestamps are different
                println("${getCurrentDateTime()}")
            }

            val folders = fakeLibraryRepository.folders.first()

            // rename folders to mix up the 'last modified' order
            fakeLibraryRepository.editFolder(
                id = folders[4].folder.id,
                updateAttributes = LibraryFolderUpdateAttributes(
                    name = "RenamedFolder1"
                )
            )

            delay(1) // necessary to ensure that the timestamps are different

            fakeLibraryRepository.editFolder(
                id = folders[2].folder.id,
                updateAttributes = LibraryFolderUpdateAttributes(
                    name = "RenamedFolder2"
                )
            )
        }
    }

    @Test
    fun `Get folders, folders are sorted by 'date added' descending`() = runTest {
        val folders = getFolders().first()

        assertThat(folders.map { it.folder.name })
            .isEqualTo(listOf(
                "RenamedFolder1",
                "TestFolder1",
                "RenamedFolder2",
                "TestFolder5",
                "TestFolder3",
            ))
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

        // Get folders
        val folders = getFolders().first()

        // Check if folders are sorted correctly
        assertThat(folders.map { it.folder.name })
            .isEqualTo(listOf(
                "TestFolder3",
                "TestFolder5",
                "RenamedFolder2",
                "TestFolder1",
                "RenamedFolder1",
            ))
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

        // Get folders
        val folders = getFolders().first()

        // Check if folders are sorted correctly
        assertThat(folders.map { it.folder.name })
            .isEqualTo(listOf(
                "RenamedFolder2",
                "RenamedFolder1",
                "TestFolder1",
                "TestFolder5",
                "TestFolder3",
            ))
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

        // Get folders
        val folders = getFolders().first()

        // Check if folders are sorted correctly
        assertThat(folders.map { it.folder.name })
            .isEqualTo(listOf(
                "TestFolder3",
                "TestFolder5",
                "TestFolder1",
                "RenamedFolder1",
                "RenamedFolder2",
            ))
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

        // Get folders
        val folders = getFolders().first()

        // Check if folders are sorted correctly
        assertThat(folders.map { it.folder.name })
            .isEqualTo(listOf(
                "TestFolder5",
                "TestFolder3",
                "TestFolder1",
                "RenamedFolder2",
                "RenamedFolder1",
            ))
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

        // Get folders
        val folders = getFolders().first()

        // Check if folders are sorted correctly
        assertThat(folders.map { it.folder.name })
            .isEqualTo(listOf(
                "RenamedFolder1",
                "RenamedFolder2",
                "TestFolder1",
                "TestFolder3",
                "TestFolder5",
            ))
    }
}