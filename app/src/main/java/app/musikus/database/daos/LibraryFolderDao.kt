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
import app.musikus.database.MusikusDatabase
import app.musikus.database.entities.LibraryFolderModel
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes

data class LibraryFolder(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "custom_order") val customOrder: Int?,
) : SoftDeleteModelDisplayAttributes() {

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        super.equals(other) &&
                (other is LibraryFolder) &&
                (other.name == name) &&
                (other.customOrder == customOrder)

    override fun hashCode() =
        (super.hashCode() *
                HASH_FACTOR + name.hashCode()) *
                HASH_FACTOR + customOrder.hashCode()
}

@Dao
abstract class LibraryFolderDao(
    database: MusikusDatabase
) : SoftDeleteDao<
        LibraryFolderModel,
        LibraryFolderUpdateAttributes,
        LibraryFolder
        >(
    tableName = "library_folder",
    database = database,
    displayAttributes = LibraryFolder::class.java.fields.map { it.name }
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: LibraryFolderModel,
        updateAttributes: LibraryFolderUpdateAttributes
    ): LibraryFolderModel = super.applyUpdateAttributes(old, updateAttributes).apply {
        name = updateAttributes.name ?: old.name
        customOrder = updateAttributes.customOrder ?: old.customOrder
    }
}