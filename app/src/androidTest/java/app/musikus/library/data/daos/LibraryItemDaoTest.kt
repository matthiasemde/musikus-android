/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 */

package app.musikus.library.data.daos

import android.database.sqlite.SQLiteConstraintException
import androidx.test.filters.SmallTest
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.data.Nullable
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.FakeTimeProvider
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.library.data.entities.LibraryItemUpdateAttributes
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SessionCreationAttributes
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
                LibraryFolderCreationAttributes("TestFolder")
            )
        }
    }

    @Test
    fun insertItems() = runTest {
        val itemIds = libraryItemDao.insert(
            listOf(
                LibraryItemCreationAttributes(
                    name = "TestItem1",
                    colorIndex = 5,
                    libraryFolderId = Nullable(null),
                ),
                LibraryItemCreationAttributes(
                    name = "TestItem2",
                    colorIndex = 0,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                )
            )
        )

        // Check if the correct ids were returned
        assertThat(itemIds).containsExactly(
            UUIDConverter.fromInt(2),
            UUIDConverter.fromInt(3)
        )

        // Check if the items were inserted correctly
        val items = libraryItemDao.getAllAsFlow().first()

        assertThat(items).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(2),
                name = "TestItem1",
                colorIndex = 5,
                libraryFolderId = null,
                customOrder = null,
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME,
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                name = "TestItem2",
                colorIndex = 0,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null,
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME,
            )
        )
    }

    @Test
    fun insertItem() = runTest {
        val item = LibraryItemCreationAttributes(
            name = "TestItem",
            colorIndex = 0,
            libraryFolderId = Nullable(null),
        )

        val libraryItemDaoSpy = spyk(libraryItemDao)

        libraryItemDaoSpy.insert(item)

        coVerify(exactly = 1) { libraryItemDaoSpy.insert(listOf(item)) }
    }

    @Test
    fun insertItemWithInvalidFolderId_throwsException() = runTest {
        val exception = assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                libraryItemDao.insert(
                    LibraryItemCreationAttributes(
                        name = "TestItem",
                        colorIndex = 0,
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
    fun updateItems() = runTest {
        // Insert items
        libraryItemDao.insert(
            listOf(
                LibraryItemCreationAttributes(
                    name = "TestItem1",
                    colorIndex = 0,
                    libraryFolderId = Nullable(null),
                ),
                LibraryItemCreationAttributes(
                    name = "TestItem2",
                    colorIndex = 5,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                )
            )
        )

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
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                name = "UpdatedItem2",
                colorIndex = 9,
                libraryFolderId = null,
                customOrder = null,
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
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
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify(exactly = 1) {
            libraryItemDaoSpy.update(
                listOf(
                    UUIDConverter.fromInt(2) to updateAttributes
                )
            )
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
            "Could not find library_item(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun updateItemWithInvalidFolderId_throwsException() = runTest {
        // Insert items
        libraryItemDao.insert(
            LibraryItemCreationAttributes(
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = Nullable(null)
            )
        )

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
        libraryItemDao.insert(
            listOf(
                LibraryItemCreationAttributes(
                    name = "TestItem1",
                    colorIndex = 0,
                    libraryFolderId = Nullable(null),
                ),
                LibraryItemCreationAttributes(
                    name = "TestItem2",
                    colorIndex = 5,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                )
            )
        )

        // Delete the items
        libraryItemDao.delete(
            listOf(
                UUIDConverter.fromInt(2),
                UUIDConverter.fromInt(3)
            )
        )

        // Check if the items were deleted correctly
        val items = libraryItemDao.getAllAsFlow().first()

        assertThat(items).isEmpty()
    }

    @Test
    fun deleteItem() = runTest {
        val libraryItemDaoSpy = spyk(libraryItemDao)

        try {
            libraryItemDaoSpy.delete(UUIDConverter.fromInt(2))
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify(exactly = 1) { libraryItemDaoSpy.delete(listOf(UUIDConverter.fromInt(2))) }
    }

    @Test
    fun deleteNonExistentItem_throwsException() = runTest {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                libraryItemDao.delete(UUIDConverter.fromInt(0))
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find library_item(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun restoreItems() = runTest {
        // Insert items
        libraryItemDao.insert(
            listOf(
                LibraryItemCreationAttributes(
                    name = "TestItem1",
                    colorIndex = 0,
                    libraryFolderId = Nullable(null),
                ),
                LibraryItemCreationAttributes(
                    name = "TestItem2",
                    colorIndex = 5,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                )
            )
        )

        // Delete the items
        libraryItemDao.delete(
            listOf(
                UUIDConverter.fromInt(2),
                UUIDConverter.fromInt(3)
            )
        )

        fakeTimeProvider.advanceTimeBy(1.seconds)

        // Restore the items
        libraryItemDao.restore(
            listOf(
                UUIDConverter.fromInt(2),
                UUIDConverter.fromInt(3)
            )
        )

        // Check if the items were restored correctly
        val items = libraryItemDao.getAllAsFlow().first()

        assertThat(items).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(2),
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = null,
                customOrder = null,
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null,
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME.plus(1.seconds.toJavaDuration()),
            )
        )
    }

    @Test
    fun restoreItem() = runTest {
        val libraryItemDaoSpy = spyk(libraryItemDao)

        try {
            libraryItemDaoSpy.restore(UUIDConverter.fromInt(2))
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify(exactly = 1) {
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
            "Could not find library_item(s) with the following id(s): [00000000-0000-0000-0000-000000000000]"
        )
    }

    @Test
    fun getSpecificItems() = runTest {
        // Insert items
        libraryItemDao.insert(
            listOf(
                LibraryItemCreationAttributes(
                    name = "TestItem1",
                    colorIndex = 0,
                    libraryFolderId = Nullable(null),
                ),
                LibraryItemCreationAttributes(
                    name = "TestItem2",
                    colorIndex = 5,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                ),
                LibraryItemCreationAttributes(
                    name = "TestItem3",
                    colorIndex = 2,
                    libraryFolderId = Nullable(null),
                ),
            )
        )

        // Get the items
        val items = libraryItemDao.getAsFlow(
            listOf(
                UUIDConverter.fromInt(2),
                UUIDConverter.fromInt(3)
            )
        ).first()

        // Check if the items were retrieved correctly
        assertThat(items).containsExactly(
            LibraryItem(
                id = UUIDConverter.fromInt(2),
                name = "TestItem1",
                colorIndex = 0,
                libraryFolderId = null,
                customOrder = null,
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME,
            ),
            LibraryItem(
                id = UUIDConverter.fromInt(3),
                name = "TestItem2",
                colorIndex = 5,
                libraryFolderId = UUIDConverter.fromInt(1),
                customOrder = null,
                createdAt = FakeTimeProvider.START_TIME,
                modifiedAt = FakeTimeProvider.START_TIME,
            )
        )
    }

    @Test
    fun getSpecificItem() = runTest {
        val libraryItemDaoSpy = spyk(libraryItemDao)

        try {
            libraryItemDaoSpy.getAsFlow(UUIDConverter.fromInt(2))
        } catch (e: IllegalArgumentException) {
            // Ignore
        }

        coVerify(exactly = 1) {
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
            "Could not find library_item(s) with the following id(s): [00000000-0000-0000-0000-000000000001]"
        )
    }

    @Test
    fun itemExists() = runTest {
        // Insert an item
        libraryItemDao.insert(
            LibraryItemCreationAttributes(
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
        libraryItemDao.insert(
            listOf(
                LibraryItemCreationAttributes(
                    name = "TestItem1",
                    colorIndex = 0,
                    libraryFolderId = Nullable(null),
                ),
                LibraryItemCreationAttributes(
                    name = "TestItem2",
                    colorIndex = 5,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                ),
                LibraryItemCreationAttributes(
                    name = "TestItem3",
                    colorIndex = 8,
                    libraryFolderId = Nullable(UUIDConverter.fromInt(1)),
                )
            )
        )

        database.sessionDao.insert(
            SessionCreationAttributes(
                breakDuration = 0.seconds,
                rating = 0,
                comment = "",
            ),
            listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(2),
                    startTimestamp = FakeTimeProvider.START_TIME,
                    duration = 1.seconds,
                )
            )
        )

        libraryItemDao.delete(
            listOf(
                UUIDConverter.fromInt(2),
                UUIDConverter.fromInt(3),
            )
        )

        // advance time by a few days
        fakeTimeProvider.advanceTimeBy(4.days)

        libraryItemDao.delete(UUIDConverter.fromInt(4))

        // advance time by just under a month and clean items
        fakeTimeProvider.advanceTimeBy(28.days)

        libraryItemDao.clean()

        // Check if the items were cleaned correctly
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                // Item with Id 2 should be still there because of the session
                // Item with Id 3 should be impossible to restore because it is permanently deleted
                // Item with Id 4 should should be still there because it was deleted less than a month ago
                libraryItemDao.restore(
                    listOf(
                        UUIDConverter.fromInt(2),
                        UUIDConverter.fromInt(3),
                        UUIDConverter.fromInt(4),
                    )
                )
            }
        }

        assertThat(exception.message).isEqualTo(
            "Could not find library_item(s) with the following id(s): [00000000-0000-0000-0000-000000000003]"
        )
    }
}
