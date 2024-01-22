/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.repository

import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.LibraryItemUpdateAttributes
import app.musikus.utils.IdProvider
import app.musikus.utils.TimeProvider
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeLibraryRepository(
    private val timeProvider: TimeProvider,
    private val idProvider: IdProvider
) : LibraryRepository {

    private val _items = mutableListOf<LibraryItem>()
    private val _folders = mutableListOf<LibraryFolderWithItems>()

    private var _itemsBuffer = listOf<LibraryItem>()
    private var _foldersBuffer = listOf<LibraryFolderWithItems>()

    override val items
        get() = flowOf(_items)

    override val folders
        get() = flowOf(_folders.map {
            it.copy(items = _items.filter { item -> item.libraryFolderId == it.folder.id })
        })

    override suspend fun addFolder(creationAttributes: LibraryFolderCreationAttributes) {
        _folders.add(LibraryFolderWithItems(
            folder = LibraryFolder(
                id = idProvider.generateId(),
                createdAt = timeProvider.now(),
                modifiedAt = timeProvider.now(),
                name = creationAttributes.name,
                customOrder = null
            ),
            items = emptyList()
        ))
    }

    override suspend fun addItem(creationAttributes: LibraryItemCreationAttributes) {
        _items.add(LibraryItem(
            id = idProvider.generateId(),
            createdAt = timeProvider.now(),
            modifiedAt = timeProvider.now(),
            name = creationAttributes.name,
            libraryFolderId = creationAttributes.libraryFolderId.value,
            colorIndex = creationAttributes.colorIndex,
            customOrder = null
        ))
    }

    override suspend fun editFolder(id: UUID, updateAttributes: LibraryFolderUpdateAttributes) {
        _folders.replaceAll { folderWithItems ->
            if (folderWithItems.folder.id == id) folderWithItems.copy(
                folder = folderWithItems.folder.copy(
                    modifiedAt = timeProvider.now(),
                    name = updateAttributes.name ?: folderWithItems.folder.name,
                    customOrder = updateAttributes.customOrder.let {
                        if (it != null) it.value
                        else folderWithItems.folder.customOrder
                    }
                )
            )
            else folderWithItems
        }
    }

    override suspend fun editItem(id: UUID, updateAttributes: LibraryItemUpdateAttributes) {
        _items.replaceAll { item ->
            if (item.id == id) item.copy(
                modifiedAt = timeProvider.now(),
                name = updateAttributes.name ?: item.name,
                libraryFolderId = updateAttributes.libraryFolderId.let {
                    if (it != null) it.value
                    else item.libraryFolderId
                },
                colorIndex = updateAttributes.colorIndex ?: item.colorIndex,
                customOrder = updateAttributes.customOrder.let {
                    if (it != null) it.value
                    else item.customOrder
                }
            )
            else item
        }
    }

    override suspend fun deleteItems(itemIds: List<UUID>) {
        _itemsBuffer = _items.filter { item -> item.id in itemIds }
        _items.removeIf { item -> item.id in itemIds }
    }

    override suspend fun deleteFolders(folderIds: List<UUID>) {
        _foldersBuffer = _folders.filter { folderWithItems -> folderWithItems.folder.id in folderIds }
        _folders.removeIf { folderWithItems -> folderWithItems.folder.id in folderIds }
    }

    override suspend fun restoreItems(itemIds: List<UUID>) {
        _items.addAll(_itemsBuffer.filter { item -> item.id in itemIds })
        _itemsBuffer = emptyList()
    }

    override suspend fun restoreFolders(folderIds: List<UUID>) {
        _folders.addAll(_foldersBuffer.filter { folderWithItems -> folderWithItems.folder.id in folderIds })
        _foldersBuffer = emptyList()
    }

    override suspend fun existsItem(id: UUID): Boolean {
        return _items.any { it.id == id }
    }

    override suspend fun existsFolder(id: UUID): Boolean {
        return _folders.any { folderWithItems -> folderWithItems.folder.id == id }
    }

    override suspend fun clean() {
        throw NotImplementedError()
    }

}