/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.repository

import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.datastore.LibraryFolderSortMode
import de.practicetime.practicetime.datastore.LibraryItemSortMode
import de.practicetime.practicetime.datastore.SortDirection
import java.util.*

class LibraryRepository(
    database: PTDatabase
) {
    private val itemDao = database.libraryItemDao
    private val folderDao = database.libraryFolderDao

    val items = itemDao.get(activeOnly = true)
    val folders = folderDao.getAllAsFlow()


    /** Mutators */
    /** Add */
    suspend fun addFolder(newFolder: LibraryFolder) {
        folderDao.insert(newFolder)
    }

    suspend fun addItem(newItem: LibraryItem) {
        itemDao.insert(newItem)
    }

    /** Edit */
    suspend fun editFolder(
        folder: LibraryFolder,
        newName: String,
    ) {
        folder.apply {
            name = newName
        }
        folderDao.update(folder)
    }

    suspend fun editItem(
        item: LibraryItem,
        newName: String,
        newColorIndex: Int,
        newFolderId: UUID?,
    ) {
        item.apply {
            name = newName
            colorIndex = newColorIndex
            libraryFolderId = newFolderId
        }
        itemDao.update(item)
    }

    /** Delete / Archive */
    suspend fun deleteFolders(folders: Set<LibraryFolder>) {
        folderDao.delete(folders.toList())
    }

    suspend fun archiveItems(items: Set<LibraryItem>) {
        items.forEach { item ->
            item.archived = true
            itemDao.update(item)
        }
    }

    /** Sort */
    fun sortFolders(
        folders: List<LibraryFolder>,
        mode: LibraryFolderSortMode,
        direction: SortDirection,
    ) = when (direction) {
        SortDirection.ASCENDING -> {
            when (mode) {
                LibraryFolderSortMode.DATE_ADDED -> folders.sortedBy { it.createdAt }
                LibraryFolderSortMode.LAST_MODIFIED -> folders.sortedBy { it.modifiedAt }
                LibraryFolderSortMode.NAME -> folders.sortedBy { it.name }
                LibraryFolderSortMode.CUSTOM -> folders // TODO
            }
        }
        SortDirection.DESCENDING -> {
            when (mode) {
                LibraryFolderSortMode.DATE_ADDED -> folders.sortedByDescending { it.createdAt }
                LibraryFolderSortMode.LAST_MODIFIED -> folders.sortedByDescending { it.modifiedAt }
                LibraryFolderSortMode.NAME -> folders.sortedByDescending { it.name }
                LibraryFolderSortMode.CUSTOM -> folders // TODO
            }
        }
    }

    fun sortItems(
        items: List<LibraryItem>,
        mode: LibraryItemSortMode,
        direction: SortDirection,
    ) = when (direction) {
        SortDirection.ASCENDING -> {
            when (mode) {
                LibraryItemSortMode.DATE_ADDED -> items.sortedBy { it.createdAt }
                LibraryItemSortMode.LAST_MODIFIED -> items.sortedBy { it.modifiedAt }
                LibraryItemSortMode.NAME -> items.sortedBy { it.name }
                LibraryItemSortMode.COLOR -> items.sortedBy { it.colorIndex }
                LibraryItemSortMode.CUSTOM -> items // TODO
            }
        }
        SortDirection.DESCENDING -> {
            when (mode) {
                LibraryItemSortMode.DATE_ADDED -> items.sortedByDescending { it.createdAt }
                LibraryItemSortMode.LAST_MODIFIED -> items.sortedByDescending { it.modifiedAt }
                LibraryItemSortMode.NAME -> items.sortedByDescending { it.name }
                LibraryItemSortMode.COLOR -> items.sortedByDescending { it.colorIndex }
                LibraryItemSortMode.CUSTOM -> items // TODO
            }
        }
    }
}