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
import kotlinx.coroutines.flow.map
import java.util.*

class LibraryRepository(
    from: String,
    database: PTDatabase
) {
    private val itemDao = database.libraryItemDao
    private val folderDao = database.libraryFolderDao

    val items = itemDao.getAllAsFlow(from).map { items -> items.sortedBy { it.name } }
    val folders = folderDao.getAllAsFlow(from).map { folders -> folders.sortedBy { it.name } }


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
    fun sortLibraryFolders(mode: LibraryFolderSortMode? = null) {
//        if(mode != null) {
//            if (mode == folderSortMode.value) {
//                when (folderSortDirection.value) {
//                    SortDirection.ASCENDING -> folderSortDirection.value = SortDirection.DESCENDING
//                    SortDirection.DESCENDING -> folderSortDirection.value = SortDirection.ASCENDING
//                }
//            } else {
//                folderSortDirection.value = SortDirection.ASCENDING
//                folderSortMode.value = mode
//                PracticeTime.prefs.edit().putString(
//                    PracticeTime.PREFERENCES_KEY_LIBRARY_FOLDER_SORT_MODE,
//                    folderSortMode.value.name
//                ).apply()
//            }
//            PracticeTime.prefs.edit().putString(
//                PracticeTime.PREFERENCES_KEY_LIBRARY_FOLDER_SORT_DIRECTION,
//                folderSortDirection.value.name
//            ).apply()
//        }
//        when (folderSortDirection.value) {
//            SortDirection.ASCENDING -> {
//                when (folderSortMode.value) {
//                    LibraryFolderSortMode.DATE_ADDED -> {
//                        _folders.update { folders -> folders.sortedBy { it.createdAt } }
//                    }
//                    LibraryFolderSortMode.LAST_MODIFIED -> {
//                        _folders.update { folders -> folders.sortedBy { it.modifiedAt } }
//                    }
//                    LibraryFolderSortMode.CUSTOM -> {}
//                }
//            }
//            SortDirection.DESCENDING -> {
//                when (folderSortMode.value) {
//                    LibraryFolderSortMode.DATE_ADDED -> {
//                        _folders.update { folders -> folders.sortedByDescending { it.createdAt } }
//                    }
//                    LibraryFolderSortMode.LAST_MODIFIED -> {
//                        _folders.update { folders -> folders.sortedByDescending { it.modifiedAt } }
//                    }
//                    LibraryFolderSortMode.CUSTOM -> {}
//                }
//            }
//        }
    }

    fun sortLibraryItems(mode: LibraryItemSortMode? = null) {
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
//        when (itemSortDirection.value) {
//            SortDirection.ASCENDING -> {
//                when (itemSortMode.value) {
//                    LibraryItemSortMode.DATE_ADDED -> {
//                        _items.update { items -> items.sortedBy { it.createdAt } }
//                    }
//                    LibraryItemSortMode.LAST_MODIFIED -> {
//                        _items.update { items -> items.sortedBy { it.modifiedAt } }
//                    }
//                    LibraryItemSortMode.NAME -> {
//                        _items.update { items -> items.sortedBy { it.name } }
//                    }
//                    LibraryItemSortMode.COLOR -> {
//                        _items.update { items -> items.sortedBy { it.colorIndex } }
//                    }
//                    LibraryItemSortMode.CUSTOM -> { }
//                }
//            }
//            SortDirection.DESCENDING -> {
//                when (itemSortMode.value) {
//                    LibraryItemSortMode.DATE_ADDED -> {
//                        _items.update { items -> items.sortedByDescending { it.createdAt } }
//                    }
//                    LibraryItemSortMode.LAST_MODIFIED -> {
//                        _items.update { items -> items.sortedByDescending { it.modifiedAt } }
//                    }
//                    LibraryItemSortMode.NAME -> {
//                        _items.update { items -> items.sortedByDescending { it.name } }
//                    }
//                    LibraryItemSortMode.COLOR -> {
//                        _items.update { items -> items.sortedByDescending { it.colorIndex } }
//                    }
//                    LibraryItemSortMode.CUSTOM -> { }
//                }
//            }
//        }
    }
}