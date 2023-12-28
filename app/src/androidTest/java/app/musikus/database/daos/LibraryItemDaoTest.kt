/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import androidx.test.filters.SmallTest
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.entities.LibraryFolderModel
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.di.AppModule
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named


@HiltAndroidTest
@UninstallModules(AppModule::class)
@SmallTest
class LibraryItemDaoTest {

    @Inject
    @Named("test_db")
    lateinit var database: MusikusDatabase
    private lateinit var libraryItemDao: LibraryItemDao

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
    fun insertItem() = runTest {
        val item = LibraryItemModel(
            name = "TestItem",
            colorIndex = 0,
            libraryFolderId = Nullable(null),
        )

        libraryItemDao.insert(item)

        val items = libraryItemDao.getAllAsFlow().first()

        assertThat(items).containsExactly(

        )
    }

    @Test
    fun insertItems() = runTest {
        val items = listOf(
            LibraryItemModel(
                name = "TestItem1",
                colorIndex = 5,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemModel(
                name = "TestItem2",
                colorIndex = 0,
                libraryFolderId = Nullable(null),
            ),
            LibraryItemModel(
                name = "TestItem3",
                colorIndex = 9,
                libraryFolderId = Nullable(null),
            ),
        )

        libraryItemDao.insert(items)

        val itemsFromDb = libraryItemDao.getAllAsFlow().first()

        assertThat(itemsFromDb.map { it.name })
            .containsExactlyElementsIn(items.map { it.name })

        assertThat(itemsFromDb.map { it.colorIndex })
            .containsExactlyElementsIn(items.map { it.colorIndex })
    }

    @Test
    fun updateItem() = runTest {
        // Insert an item
        val item = LibraryItemModel(
            name = "TestItem",
            colorIndex = 0,
            libraryFolderId = Nullable(null),
        )
        libraryItemDao.insert(item)

        // Get the item from the database
        val items = libraryItemDao.getAllAsFlow().first()

        // Update the item
        val folderId = UUID.fromString("00000000-0000-0000-0000-000000000001")

        val itemUpdatedAttributes = LibraryItemUpdateAttributes(
            name = "UpdatedItem",
            colorIndex = 1,
            libraryFolderId = Nullable(folderId),
        )

        libraryItemDao.update(
            id = items.first().id,
            updateAttributes = itemUpdatedAttributes
        )

        // Check if the item was updated correctly
        val updatedItems = libraryItemDao.getAllAsFlow().first()

        assertThat(updatedItems.first().name).isEqualTo("UpdatedItem")
        assertThat(updatedItems.first().colorIndex).isEqualTo(1)
        assertThat(updatedItems.first().libraryFolderId).isEqualTo(folderId)
    }
}