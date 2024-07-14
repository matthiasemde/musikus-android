/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.library.data

import app.musikus.core.data.LibraryFolderWithItems
import app.musikus.library.data.daos.LibraryFolderDao
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.daos.LibraryItemDao
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryFolderUpdateAttributes
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.library.data.entities.LibraryItemUpdateAttributes
import app.musikus.library.domain.LibraryRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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