/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.repository

import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryFolderDao
import app.musikus.database.daos.LibraryItem
import app.musikus.database.daos.LibraryItemDao
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderModel
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.LibraryItemUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface LibraryRepository {
    val items: Flow<List<LibraryItem>>
    val folders: Flow<List<LibraryFolder>>

    /** Mutators */
    /** Add */
    suspend fun addFolder(creationAttributes: LibraryFolderCreationAttributes)
    suspend fun addItem(creationAttributes: LibraryItemCreationAttributes)

    /** Edit */
    suspend fun editFolder(id: UUID, updateAttributes: LibraryFolderUpdateAttributes)
    suspend fun editItem(id: UUID, updateAttributes: LibraryItemUpdateAttributes)

    /** Delete / restore */
    suspend fun deleteItems(items: List<LibraryItem>)
    suspend fun deleteFolders(folders: List<LibraryFolder>)

    suspend fun restoreItems(items: List<LibraryItem>)
    suspend fun restoreFolders(folders: List<LibraryFolder>)

    /** Clean */
    suspend fun clean()
}

class LibraryRepositoryImpl(
    private val itemDao: LibraryItemDao,
    private val folderDao: LibraryFolderDao,
) : LibraryRepository {

    override val items = itemDao.getAllAsFlow()
    override val folders = folderDao.getAllAsFlow()


    /** Mutators */
    /** Add */
    override suspend fun addFolder(creationAttributes: LibraryFolderCreationAttributes) {
        folderDao.insert(
            LibraryFolderModel(
                name = creationAttributes.name,
            )
        )
    }

    override suspend fun addItem(creationAttributes: LibraryItemCreationAttributes) {
        itemDao.insert(
            LibraryItemModel(
                name = creationAttributes.name,
                colorIndex = creationAttributes.colorIndex,
                libraryFolderId = creationAttributes.libraryFolderId,
            )
        )
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
    override suspend fun deleteItems(items: List<LibraryItem>) {
        itemDao.delete(items.map { it.id })
    }

    override suspend fun restoreItems(items: List<LibraryItem>) {
        itemDao.restore(items.map{ it.id })
    }

    override suspend fun deleteFolders(folders: List<LibraryFolder>) {
        folderDao.delete(folders.map { it.id })
    }

    override suspend fun restoreFolders(folders: List<LibraryFolder>) {
        folderDao.restore(folders.map { it.id })
    }

    /** Clean */
    override suspend fun clean() {
        folderDao.clean()
        itemDao.clean()
    }
}