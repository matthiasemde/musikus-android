/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import app.musikus.database.PTDatabase
import app.musikus.database.SoftDeleteDao
import app.musikus.database.SoftDeleteModelDisplayAttributes
import app.musikus.database.entities.LibraryFolderModel
import app.musikus.database.entities.LibraryFolderUpdateAttributes

data class LibraryFolder(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "order") val order: Int?,
) : SoftDeleteModelDisplayAttributes()

@Dao
abstract class LibraryFolderDao(
    database: PTDatabase
) : SoftDeleteDao<
    LibraryFolderModel,
    LibraryFolderUpdateAttributes,
    LibraryFolder
>(
    tableName = "library_folder",
    database = database,
    displayAttributes = listOf("name", "order")
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: LibraryFolderModel,
        updateAttributes: LibraryFolderUpdateAttributes
    ): LibraryFolderModel = super.applyUpdateAttributes(old, updateAttributes).apply {
        name = updateAttributes.name ?: old.name
        order = updateAttributes.order ?: old.order
    }
}