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
    from: String,
    database: PTDatabase
) {
    private val itemDao = database.libraryItemDao
    private val folderDao = database.libraryFolderDao

    val items = itemDao.getAllAsFlow(from)
    val folders = folderDao.getAllAsFlow(from)


    /** Mutators */
    /** Add */
    fun addFolder(newFolder: LibraryFolder) {
        folderDao.insert(newFolder)
    }

    fun addItem(newItem: LibraryItem) {
        itemDao.insert(newItem)
    }

    /** Edit */
    fun editFolder(editedFolder: LibraryFolder) {
        folderDao.update(editedFolder)
    }

    fun editItem(item: LibraryItem) {
        itemDao.update(item)
    }

    /** Delete / Archive */
    fun deleteFolders(folderIds: List<UUID>) {
//        folderDao.getAndDelete(folderIds)
//        loadLibraryItems() // reload items to show those which were in a folder
    }

    fun archiveItems(itemIds: List<UUID>) {
        itemIds.forEach { itemId ->
            itemDao.archive(itemId)
        }
    }

    /** Sort */
    fun sortFolders(
        folders: List<LibraryFolder>,
        mode: LibraryFolderSortMode,
        direction: SortDirection,
    ) =

    when (direction) {
        SortDirection.ASCENDING -> {
            when (mode) {
                LibraryFolderSortMode.DATE_ADDED -> folders.sortedBy { it.createdAt }
                LibraryFolderSortMode.LAST_MODIFIED -> folders.sortedBy { it.modifiedAt }
                LibraryFolderSortMode.CUSTOM -> folders // TODO
            }
        }
        SortDirection.DESCENDING -> {
            when (mode) {
                LibraryFolderSortMode.DATE_ADDED -> folders.sortedByDescending { it.createdAt }
                LibraryFolderSortMode.LAST_MODIFIED -> folders.sortedByDescending { it.modifiedAt }
                LibraryFolderSortMode.CUSTOM -> folders // TODO
            }
        }
    }

    fun sortItems(
        items: List<LibraryItem>,
        mode: LibraryItemSortMode,
        direction: SortDirection,
    ) =
//        if(mode != null) {
//            if (mode == itemSortMode.value) {
//                when (itemSortDirection.value) {
//                    SortDirection.ASCENDING -> itemSortDirection.value = SortDirection.DESCENDING
//                    SortDirection.DESCENDING -> itemSortDirection.value = SortDirection.ASCENDING
//                }
//            } else {
//                itemSortDirection.value = SortDirection.ASCENDING
//                itemSortMode.value = mode
//                PracticeTime.prefs.edit().putString(
//                    PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_MODE,
//                    itemSortMode.value.name
//                ).apply()
//            }
//            PracticeTime.prefs.edit().putString(
//                PracticeTime.PREFERENCES_KEY_LIBRARY_ITEM_SORT_DIRECTION,
//                itemSortDirection.value.name
//            ).apply()
//        }
    when (direction) {
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