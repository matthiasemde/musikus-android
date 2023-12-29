/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import android.database.sqlite.SQLiteConstraintException
import androidx.test.filters.SmallTest
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.LibraryFolderModel
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.LibraryItemUpdateAttributes
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


@HiltAndroidTest
@UninstallModules(AppModule::class)
@SmallTest
class LibraryItemDaoTest {

    @Inject
    @Named("test_db")
    lateinit var database: MusikusDatabase
    private lateinit var libraryItemDao: LibraryItemDao

    @Inject lateinit var fakeTimeProvider: FakeTimeProvider

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()

        libraryItemDao = database.libraryItemDao

        // Insert a folder so we can test moving items into it
        runBlocking {
            database.libraryFolderDao.insert(
                LibraryFolderModel("TestFolder")
            )
        }
    }

    @Test
    fun insertItems() = runTest {

        libraryItemDao.insert(listOf(
            LibraryItemModel(
                name = "TestItem1",
                colorIndex = 5,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemModel(
                name = "TestItem2",
                colorIndex = 0,
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
            )
        ))

        val items = libraryItemDao.getAllAsFlow().first()

        assertThat(items).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(2),
                name = "TestItem1",
                colorIndex = 5,
                libraryFolderId = null,
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                name = "TestItem2",
                colorIndex = 0,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
            )
        )
    }

    @Test
    fun insertItem() = runTest {
        val item = LibraryItemModel(
            name = "TestItem",
            colorIndex = 0,
            libraryFolderId = Nullable(null),
        )

        val libraryItemDaoSpy = spyk(libraryItemDao)

        libraryItemDaoSpy.insert(item)

        coVerify (exactly = 1) { libraryItemDaoSpy.insert(listOf(item)) }
    }

    @Test
    fun insertItemWithInvalidFolderId_throwsException() = runTest {
        val item = LibraryItemModel(
            name = "TestItem",
            colorIndex = 0,
            libraryFolderId = Nullable(UUIDConverter.fromInt(0)),
        )

        val exception = assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                libraryItemDao.insert(item)
            }
        }

        assertThat(exception.message).isEqualTo(
            "FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)"
        )
    }

    @Test
    fun updateItems() = runTest {
        // Insert items
        libraryItemDao.insert(listOf(
            LibraryItemModel(
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemModel(
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
            )
        ))

        fakeTimeProvider.advanceTimeBy(1.seconds)

        // Update the items
        libraryItemDao.update(
            listOf(
                UUIDConverter.fromInt(2) to LibraryItemUpdateAttributes(
                    name = "UpdatedItem1",
                    colorIndex = 0,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                ),
                UUIDConverter.fromInt(3) to LibraryItemUpdateAttributes(
                    name = "UpdatedItem2",
                    colorIndex = 9,
                    libraryFolderId = Nullable(null),
                )
            )
        )

        // Check if the items were updated correctly
        val updatedItems = libraryItemDao.getAllAsFlow().first()

        assertThat(updatedItems).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(2),
                name = "UpdatedItem1",
                colorIndex = 0,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                name = "UpdatedItem2",
                colorIndex = 9,
                libraryFolderId = null,
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            )
        )
    }

    @Test
    fun updateItem() = runTest {
        val updateAttributes = LibraryItemUpdateAttributes(
            name = "UpdatedItem1",
            colorIndex = 0,
            libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
        )

        val libraryItemDaoSpy = spyk(libraryItemDao)

        try {
            libraryItemDaoSpy.update(
                UUIDConverter.fromInt(2),
                updateAttributes
            )
        } catch (e: Exception) {
            // Ignore
        }

        coVerify (exactly = 1) {
            libraryItemDaoSpy.update(listOf(
                UUIDConverter.fromInt(2) to updateAttributes
            ))
        }
    }

    @Test
    fun updateNonExistentItem_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryItemDao.update(
                    UUIDConverter.fromInt(0),
                    LibraryItemUpdateAttributes()
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun updateItemWithInvalidFolderId_throwsException() = runTest {
        // Insert items
        libraryItemDao.insert(LibraryItemModel(
            name = "TestItem1",
            colorIndex = 0,
            libraryFolderId = Nullable(null)
        ))

        val exception = assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                libraryItemDao.update(
                    UUIDConverter.fromInt(2),
                    LibraryItemUpdateAttributes(
                        libraryFolderId = Nullable(UUIDConverter.fromInt(0)),
                    )
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)"
        )
    }

    @Test
    fun deleteItems() = runTest {
        // Insert items
        libraryItemDao.insert(listOf(
            LibraryItemModel(
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemModel(
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
            )
        ))

        // Delete the items
        libraryItemDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3)
        ))

        // Check if the items were deleted correctly
        val items = libraryItemDao.getAllAsFlow().first()

        assertThat(items).isEmpty()
    }

    @Test
    fun deleteItem() = runTest {
        val libraryItemDaoSpy = spyk(libraryItemDao)

        try {
            libraryItemDaoSpy.delete(UUIDConverter.fromInt(2))
        } catch (e: Exception) {
            // Ignore
        }

        coVerify (exactly = 1) { libraryItemDaoSpy.delete(listOf(UUIDConverter.fromInt(2))) }
    }

    @Test
    fun deleteNonExistentItem_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryItemDao.delete(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun restoreItems() = runTest {
        // Insert items
        libraryItemDao.insert(listOf(
            LibraryItemModel(
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemModel(
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
            )
        ))

        // Delete the items
        libraryItemDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3)
        ))

        fakeTimeProvider.advanceTimeBy(1.seconds)

        // Restore the items
        libraryItemDao.restore(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3)
        ))

        // Check if the items were restored correctly
        val items = libraryItemDao.getAllAsFlow().first()

        assertThat(items).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(2),
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = null,
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime.plus(1.seconds.toJavaDuration()),
            )
        )
    }

    @Test
    fun restoreItem() = runTest {
        val libraryItemDaoSpy = spyk(libraryItemDao)

        try {
            libraryItemDaoSpy.restore(UUIDConverter.fromInt(2))
        } catch (e: Exception) {
            // Ignore
        }

        coVerify (exactly = 1) {
            libraryItemDaoSpy.restore(listOf(UUIDConverter.fromInt(2)))
        }
    }

    @Test
    fun restoreNonExistentItem_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryItemDao.restore(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun getSpecificItems() = runTest {
        // Insert items
        libraryItemDao.insert(listOf(
            LibraryItemModel(
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemModel(
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
            ),
            LibraryItemModel(
                name = "TestItem3",
                colorIndex = 2,
                libraryFolderId = Nullable(null),
            ),
        ))

        // Get the items
        val items = libraryItemDao.getAsFlow(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3)
        )).first()

        // Check if the items were retrieved correctly
        assertThat(items).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(2),
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = null,
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null,
                createdAt = fakeTimeProvider.startTime,
                modifiedAt = fakeTimeProvider.startTime,
            )
        )
    }

    @Test
    fun getSpecificItem() = runTest {
        val libraryItemDaoSpy = spyk(libraryItemDao)

        try {
            libraryItemDaoSpy.getAsFlow(UUIDConverter.fromInt(2))
        } catch (e: Exception) {
            // Ignore
        }

        coVerify (exactly = 1) {
            libraryItemDaoSpy.getAsFlow(listOf(UUIDConverter.fromInt(2)))
        }
    }

    @Test
    fun getNonExistentItem_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryItemDao.getAsFlow(UUIDConverter.fromInt(1)).first()
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun itemExists() = runTest {
        // Insert an item
        libraryItemDao.insert(
            LibraryItemModel(
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = Nullable(null),
            )
        )

        // Check if the item exists
        assertThat(libraryItemDao.exists(UUIDConverter.fromInt(2))).isTrue()
    }

    @Test
    fun itemDoesNotExist() = runTest {
        assertThat(libraryItemDao.exists(UUIDConverter.fromInt(1))).isFalse()
    }

    @Test
    fun cleanItems() = runTest {
        libraryItemDao.insert(listOf(
            LibraryItemModel(
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemModel(
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
            )
        ))

        libraryItemDao.delete(listOf(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3)
        ))

        // move to the next month
        fakeTimeProvider.advanceTimeBy((24 * 32).hours)

        libraryItemDao.clean()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                // Restoring cleaned items should be impossible
                libraryItemDao.restore(listOf(
                    UUIDConverter.fromInt(2),
                    UUIDConverter.fromInt(3)
                ))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find the following id(s): [" +
                    "00000000-0000-0000-0000-000000000002, " +
                    "00000000-0000-0000-0000-000000000003" +
                    "]"
        )
    }
}