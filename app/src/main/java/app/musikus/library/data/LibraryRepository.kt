/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.library.data

import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.daos.LibraryFolderDao
import app.musikus.database.daos.LibraryItem
import app.musikus.database.daos.LibraryItemDao
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface LibraryRepository {
    val items: Flow<List<LibraryItem>>
    val folders: Flow<List<LibraryFolderWithItems>>

    /** Mutators */
    /** Add */
    suspend fun addFolder(creationAttributes: LibraryFolderCreationAttributes)
    suspend fun addItem(creationAttributes: LibraryItemCreationAttributes)

    /** Edit */
    suspend fun editFolder(id: UUID, updateAttributes: LibraryFolderUpdateAttributes)
    suspend fun editItem(id: UUID, updateAttributes: LibraryItemUpdateAttributes)

    /** Delete / restore */
    suspend fun deleteItems(itemIds: List<UUID>)
    suspend fun deleteFolders(folderIds: List<UUID>)

    suspend fun restoreItems(itemIds: List<UUID>)
    suspend fun restoreFolders(folderIds: List<UUID>)

    /** Exists */
    suspend fun existsItem(id: UUID): Boolean
    suspend fun existsFolder(id: UUID): Boolean

    /** Clean */
    suspend fun clean()
}

class LibraryRepositoryImpl(
    private val itemDao: LibraryItemDao,
    private val folderDao: LibraryFolderDao,
) : LibraryRepository {

    override val items = itemDao.getAllAsFlow()
    override val folders = folderDao.getAllWithItems()


    /** Mutators */
    /** Add */
    override suspend fun addFolder(creationAttributes: LibraryFolderCreationAttributes) {
        folderDao.insert(creationAttributes)
    }

    override suspend fun addItem(creationAttributes: LibraryItemCreationAttributes) {
        itemDao.insert(creationAttributes)
    }

    /** Edit */
    override suspend fun editFolder(
        id: UUID,
        updateAttributes: LibraryFolderUpdateAttributes
    ) {
        folderDao.update(id, updateAttributes)
    }

    override suspend fun editItem(
        id: UUID,
        updateAttributes: LibraryItemUpdateAttributes
    ) {
        itemDao.update(id, updateAttributes)
    }

    /** Delete / restore */
    override suspend fun deleteItems(itemIds: List<UUID>) {
        itemDao.delete(itemIds)
    }

    override suspend fun restoreItems(itemIds: List<UUID>) {
        itemDao.restore(itemIds)
    }

    override suspend fun deleteFolders(folderIds: List<UUID>) {
        folderDao.delete(folderIds)
    }

    override suspend fun restoreFolders(folderIds: List<UUID>) {
        folderDao.restore(folderIds)
    }

    /** Exists */
    override suspend fun existsItem(id: UUID): Boolean {
        return itemDao.exists(id)
    }

    override suspend fun existsFolder(id: UUID): Boolean {
        return folderDao.exists(id)
    }

    /** Clean */
    override suspend fun clean() {
        folderDao.clean()
        itemDao.clean()
    }
}