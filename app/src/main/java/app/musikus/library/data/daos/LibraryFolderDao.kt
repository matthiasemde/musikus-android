/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.library.data.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.musikus.core.data.daos.SoftDeleteDao
import app.musikus.core.data.LibraryFolderWithItems
import app.musikus.core.data.MusikusDatabase
import app.musikus.library.data.entities.LibraryFolderCreationAttributes
import app.musikus.library.data.entities.LibraryFolderModel
import app.musikus.library.data.entities.LibraryFolderUpdateAttributes
import app.musikus.core.data.entities.SoftDeleteModelDisplayAttributes
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

    override fun toString(): String {
        return super.toString() +
                "\tname:\t\t\t\t\t$name\n" +
                "\tcustomOrder:\t\t\t$customOrder\n"
    }
}

@Dao
abstract class LibraryFolderDao(
    database: MusikusDatabase,
) : SoftDeleteDao<
        LibraryFolderModel,
        LibraryFolderCreationAttributes,
        LibraryFolderUpdateAttributes,
        LibraryFolder
        >(
    tableName = "library_folder",
    database = database,
    displayAttributes = listOf("name", "custom_order")
) {

    /**
     * @Insert
     */

    override fun createModel(creationAttributes: LibraryFolderCreationAttributes): LibraryFolderModel {
        return LibraryFolderModel(
            name = creationAttributes.name,
        )
    }


    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        oldModel: LibraryFolderModel,
        updateAttributes: LibraryFolderUpdateAttributes
    ): LibraryFolderModel = super.applyUpdateAttributes(oldModel, updateAttributes).apply {
        name = updateAttributes.name ?: oldModel.name
        customOrder = updateAttributes.customOrder ?: oldModel.customOrder
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