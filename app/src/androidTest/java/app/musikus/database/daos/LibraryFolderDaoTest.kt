/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import androidx.test.filters.SmallTest
import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.di.AppModule
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


@HiltAndroidTest
@UninstallModules(AppModule::class)
@SmallTest
class LibraryFolderDaoTest {

    @Inject
    @Named("test_db")
    lateinit var database: MusikusDatabase
    private lateinit var libraryFolderDao: LibraryFolderDao

    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()

        libraryFolderDao = database.libraryFolderDao
    }

    @Test
    fun insertFolders() = runTest {

        val folderIds = libraryFolderDao.insert(listOf(
            LibraryFolderCreationAttributes(name = "TestFolder1"),
            LibraryFolderCreationAttributes(name = "TestFolder2")
        ))

        // Check if the folderIds were returned correctly
        assertThat(folderIds).containsExactly(
            UUIDConverter.fromInt(1),
            UUIDConverter.fromInt(2)
        )

        // Check if the folders were inserted correctly
        val folders = libraryFolderDao.getAllAsFlow().first()

        assertThat(folders).containsExactly(
            LibraryFolder(
                id = UUIDConverter.fromInt(1),
                name = "TestFolder1",
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
            ),
            LibraryFolder(
                id = UUIDConverter.fromInt(2),
                name = "TestFolder2",
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
            )
        )
    }

    @Test
    fun insertFolder() = runTest {
        val folder = LibraryFolderCreationAttributes(name = "TestFolder")

        val libraryFolderDaoSpy = spyk(libraryFolderDao)

        libraryFolderDaoSpy.insert(folder)

        coVerify (exactly = 1) { libraryFolderDaoSpy.insert(listOf(folder)) }
    }

    @Test
    fun updateFolders() = runTest {
        // Insert folders
        libraryFolderDao.insert(listOf(
            LibraryFolderCreationAttributes(name = "TestFolder1"),
            LibraryFolderCreationAttributes(name = "TestFolder2")
        ))

        fakeTimeProvider.advanceTimeBy(1.seconds)

        // Update the folders
        libraryFolderDao.update(
            listOf(
                UUIDConverter.fromInt(1) to LibraryFolderUpdateAttributes(
                    name = "UpdatedFolder1",
                ),
                UUIDConverter.fromInt(2) to LibraryFolderUpdateAttributes(
                    name = "UpdatedFolder2",
                )
            )
        )

        // Check if the folders were updated correctly
        val updatedFolders = libraryFolderDao.getAllAsFlow().first()

        assertThat(updatedFolders).containsExactly(
            LibraryFolder(
                id = UUIDConverter.fromInt(1),
                name = "UpdatedFolder1",
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            ),
            LibraryFolder(
                id = UUIDConverter.fromInt(2),
                name = "UpdatedFolder2",
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            )
        )
    }

    @Test
    fun updateFolder() = runTest {
        val updateAttributes = LibraryFolderUpdateAttributes("UpdatedFolder1")

        val libraryFolderDaoSpy = spyk(libraryFolderDao)

        try {
            libraryFolderDaoSpy.update(
                UUIDConverter.fromInt(1),
                updateAttributes
            )
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify (exactly = 1) {
            libraryFolderDaoSpy.update(listOf(
                UUIDConverter.fromInt(1) to updateAttributes
            ))
        }
    }

    @Test
    fun updateNonExistentFolder_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryFolderDao.update(
                    UUIDConverter.fromInt(0),
                    LibraryFolderUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find library_folder(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun deleteFolders() = runTest {
        // Insert folders
        libraryFolderDao.insert(listOf(
            LibraryFolderCreationAttributes(name = "TestFolder1"),
            LibraryFolderCreationAttributes(name = "TestFolder2")
        ))

        // Delete the folders
        libraryFolderDao.delete(listOf(
            UUIDConverter.fromInt(1),
            UUIDConverter.fromInt(2)
        ))

        // Check if the folders were deleted correctly
        val folders = libraryFolderDao.getAllAsFlow().first()

        assertThat(folders).isEmpty()
    }

    @Test
    fun deleteFolder() = runTest {
        val libraryFolderDaoSpy = spyk(libraryFolderDao)

        try {
            libraryFolderDaoSpy.delete(UUIDConverter.fromInt(2))
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify (exactly = 1) { libraryFolderDaoSpy.delete(listOf(UUIDConverter.fromInt(2))) }
    }

    @Test
    fun deleteNonExistentFolder_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryFolderDao.delete(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find library_folder(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun restoreFolders() = runTest {
        // Insert folders
        libraryFolderDao.insert(listOf(
            LibraryFolderCreationAttributes(name = "TestFolder1"),
            LibraryFolderCreationAttributes(name = "TestFolder2")
        ))

        // Delete the folders
        libraryFolderDao.delete(listOf(
            UUIDConverter.fromInt(1),
            UUIDConverter.fromInt(2)
        ))

        fakeTimeProvider.advanceTimeBy(1.seconds)

        // Restore the folders
        libraryFolderDao.restore(listOf(
            UUIDConverter.fromInt(1),
            UUIDConverter.fromInt(2)
        ))

        // Check if the folders were restored correctly
        val folders = libraryFolderDao.getAllAsFlow().first()

        assertThat(folders).containsExactly(
            LibraryFolder(
                id = UUIDConverter.fromInt(1),
                name = "TestFolder1",
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            ),
            LibraryFolder(
                id = UUIDConverter.fromInt(2),
                name = "TestFolder2",
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            )
        )
    }

    @Test
    fun restoreFolder() = runTest {
        val libraryFolderDaoSpy = spyk(libraryFolderDao)

        try {
            libraryFolderDaoSpy.restore(UUIDConverter.fromInt(2))
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify (exactly = 1) {
            libraryFolderDaoSpy.restore(listOf(UUIDConverter.fromInt(2)))
        }
    }

    @Test
    fun restoreNonExistentFolder_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryFolderDao.restore(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find library_folder(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun getSpecificFolders() = runTest {
        // Insert folders
        libraryFolderDao.insert(listOf(
            LibraryFolderCreationAttributes(name = "TestFolder1"),
            LibraryFolderCreationAttributes(name = "TestFolder2"),
            LibraryFolderCreationAttributes(name = "TestFolder3"),
        ))

        // Get the folders
        val folders = libraryFolderDao.getAsFlow(listOf(
            UUIDConverter.fromInt(1),
            UUIDConverter.fromInt(3)
        )).first()

        // Check if the folders were retrieved correctly
        assertThat(folders).containsExactly(
            LibraryFolder(
                id = UUIDConverter.fromInt(1),
                name = "TestFolder1",
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
            ),
            LibraryFolder(
                id = UUIDConverter.fromInt(3),
                name = "TestFolder3",
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
            )
        )
    }

    @Test
    fun getSpecificFolder() = runTest {
        val libraryFolderDaoSpy = spyk(libraryFolderDao)

        try {
            libraryFolderDaoSpy.getAsFlow(UUIDConverter.fromInt(2))
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify (exactly = 1) {
            libraryFolderDaoSpy.getAsFlow(listOf(UUIDConverter.fromInt(2)))
        }
    }

    @Test
    fun getNonExistentFolder_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryFolderDao.getAsFlow(UUIDConverter.fromInt(1)).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find library_folder(s) with the following id(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun folderExists() = runTest {
        // Insert a folder
        libraryFolderDao.insert(LibraryFolderCreationAttributes(name = "TestFolder1"))

        // Check if the folder exists
        assertThat(libraryFolderDao.exists(UUIDConverter.fromInt(1))).isTrue()
    }

     @Test
     fun deletedFolderDoesNotExist() = runTest {
        // Insert a folder
        libraryFolderDao.insert(LibraryFolderCreationAttributes(name = "TestFolder1"))

        // Delete the folder
        libraryFolderDao.delete(UUIDConverter.fromInt(1))

        // Check if the folder exists
        assertThat(libraryFolderDao.exists(UUIDConverter.fromInt(1))).isFalse()
     }

    @Test
    fun folderDoesNotExist() = runTest {
        assertThat(libraryFolderDao.exists(UUIDConverter.fromInt(1))).isFalse()
    }

    @Test
    fun cleanFolders() = runTest {
        libraryFolderDao.insert(listOf(
            LibraryFolderCreationAttributes(name = "TestFolder1"),
            LibraryFolderCreationAttributes(name = "TestFolder2"),
        ))

        libraryFolderDao.delete(UUIDConverter.fromInt(1))

        // advance time by a few days
        fakeTimeProvider.advanceTimeBy(4.days)

        libraryFolderDao.delete(UUIDConverter.fromInt(2))

        // advance time by just under a month and clea folders
        fakeTimeProvider.advanceTimeBy(28.days)

        libraryFolderDao.clean()

        // Check if the folders were cleaned correctly
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                // Folder with Id 1 should be impossible to restore because it is permanently deleted
                // Folder with Id 2 should should be still there because it was deleted less than a month ago
                libraryFolderDao.restore(listOf(
                    UUIDConverter.fromInt(1),
                    UUIDConverter.fromInt(2),
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find library_folder(s) with the following id(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun getFoldersWithItems() = runTest {
        libraryFolderDao.insert(LibraryFolderCreationAttributes(name = "TestFolder1"))

        database.libraryItemDao.insert(listOf(
            LibraryItemCreationAttributes(
                name = "TestItem1",
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                colorIndex = 6,
            ),
            LibraryItemCreationAttributes(
                name = "TestItem2",
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                colorIndex = 3,
            )
        ))

        val foldersWithItems = libraryFolderDao.getAllWithItems().first()

        assertThat(foldersWithItems).containsExactly(
            LibraryFolderWithItems(
                folder = LibraryFolder(
                    id = UUIDConverter.fromInt(1),
                    name = "TestFolder1",
                    customOrder = null,
                    createdAt = fakeTimeProvider.startTime,
                    modifiedAt = fakeTimeProvider.startTime,
                ),
                items = listOf(
                    LibraryItem(
                        id = UUIDConverter.fromInt(2),
                        name = "TestItem1",
                        colorIndex = 6,
                        libraryFolderId = UUIDConverter.fromInt(1),
                        customOrder = null,
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                    ),
                    LibraryItem(
                        id = UUIDConverter.fromInt(3),
                        name = "TestItem2",
                        colorIndex = 3,
                        libraryFolderId = UUIDConverter.fromInt(1),
                        customOrder = null,
                        createdAt = fakeTimeProvider.startTime,
                        modifiedAt = fakeTimeProvider.startTime,
                    )
                )
            )
        )
    }
}