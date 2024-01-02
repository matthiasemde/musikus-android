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
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.musikus.database.LibraryFolderWithItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.entities.LibraryFolderModel
import app.musikus.database.entities.LibraryFolderUpdateAttributes
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

data class LibraryFolder(
    @ColumnInfo(name="id") override val id: UUID,
    @ColumnInfo(name="created_at") override val createdAt: ZonedDateTime,
    @ColumnInfo(name="modified_at") override val modifiedAt: ZonedDateTime,
    @ColumnInfo(name="name") val name: String,
    @ColumnInfo(name="custom_order") val customOrder: Int?,
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

    override fun toString(): String {
        return super.toString() +
                "\tname:\t\t\t\t$name\n" +
                "\tcustomOrder:\t\t$customOrder\n"
    }
}

@Dao
abstract class LibraryFolderDao(
    database: MusikusDatabase,
) : SoftDeleteDao<
        LibraryFolderModel,
        LibraryFolderUpdateAttributes,
        LibraryFolder
        >(
    tableName = "library_folder",
    database = database,
    displayAttributes = listOf("name", "custom_order")
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

    /**
     * @Query
     */
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM library_folder " +
            "WHERE deleted = 0")
    abstract fun getAllWithItems(): Flow<List<LibraryFolderWithItems>>
}

class InvalidLibraryFolderException(message: String): Exception(message)