/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.repository

import app.musikus.database.MusikusDatabase
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderModel
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.LibraryItemUpdateAttributes
import java.util.UUID

class LibraryRepository(
    database: MusikusDatabase
) {
    private val itemDao = database.libraryItemDao
    private val folderDao = database.libraryFolderDao

    val items = itemDao.getAllAsFlow()
    val folders = folderDao.getAllAsFlow()


    /** Mutators */
    /** Add */
    suspend fun addFolder(creationAttributes: LibraryFolderCreationAttributes) {
        folderDao.insert(
            LibraryFolderModel(
                name = creationAttributes.name,
            )
        )
    }

    suspend fun addItem(creationAttributes: LibraryItemCreationAttributes) {
        itemDao.insert(
            LibraryItemModel(
                name = creationAttributes.name,
                colorIndex = creationAttributes.colorIndex,
                libraryFolderId = creationAttributes.libraryFolderId,
            )
        )
    }

    /** Edit */
    suspend fun editFolder(
        id: UUID,
        updateAttributes: LibraryFolderUpdateAttributes
    ) {
        folderDao.update(id, updateAttributes)
    }

    suspend fun editItem(
        id: UUID,
        updateAttributes: LibraryItemUpdateAttributes
    ) {
        itemDao.update(id, updateAttributes)
    }

    /** Delete / restore */
    suspend fun deleteItems(items: List<LibraryItem>) {
        itemDao.delete(items.map { it.id })
    }

    suspend fun restoreItems(items: List<LibraryItem>) {
        itemDao.restore(items.map{ it.id })
    }

    suspend fun deleteFolders(folders: List<LibraryFolder>) {
        folderDao.delete(folders.map { it.id })
    }

    suspend fun restoreFolders(folders: List<LibraryFolder>) {
        folderDao.restore(folders.map { it.id })
    }

    /** Clean */
    suspend fun clean() {
        folderDao.clean()
        itemDao.clean()
    }
}