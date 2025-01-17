/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.library.domain

import app.musikus.core.data.LibraryFolderWithItems
import app.musikus.library.data.daos.LibraryItem
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryFolderUpdateAttributes
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.library.data.entities.LibraryItemUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface LibraryRepository {
    val items: Flow<List<LibraryItem>>
    val folders: Flow<List<LibraryFolderWithItems>>

    /** Mutators */
    /** Add */
    suspend fun addFolder(creationAttributes: LibraryFolderCreationAttributes): UUID
    suspend fun addItem(creationAttributes: LibraryItemCreationAttributes): UUID

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
