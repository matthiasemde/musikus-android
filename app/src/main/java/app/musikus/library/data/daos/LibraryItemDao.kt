/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023-2024 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.library.data.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.data.daos.SoftDeleteDao
import app.musikus.core.data.entities.SoftDeleteModelDisplayAttributes
import app.musikus.core.data.toDatabaseInterpretableString
import app.musikus.library.data.entities.LibraryItemCreationAttributes
import app.musikus.library.data.entities.LibraryItemModel
import app.musikus.library.data.entities.LibraryItemUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

data class LibraryItem(
    @ColumnInfo(name = "id") override val id: UUID,
    @ColumnInfo(name = "created_at") override val createdAt: ZonedDateTime,
    @ColumnInfo(name = "modified_at") override val modifiedAt: ZonedDateTime,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    @ColumnInfo(name = "library_folder_id") val libraryFolderId: UUID?,
    @ColumnInfo(name = "custom_order") val customOrder: Int?,
) : SoftDeleteModelDisplayAttributes() {

    override fun toString(): String {
        return super.toString() +
            "\tname:\t\t\t\t\t$name\n" +
            "\tcolorIndex:\t\t\t\t$colorIndex\n" +
            "\tlibraryFolderId:\t\t$libraryFolderId\n" +
            "\tcustomOrder:\t\t\t$customOrder\n"
    }
}

@Dao
abstract class LibraryItemDao(
    private val database: MusikusDatabase
) : SoftDeleteDao<
    LibraryItemModel,
    LibraryItemCreationAttributes,
    LibraryItemUpdateAttributes,
    LibraryItem
    >(
    tableName = "library_item",
    database = database,
    displayAttributes = listOf("name", "color_index", "library_folder_id", "custom_order")
) {

    /**
     * @Insert
     */

    override fun createModel(creationAttributes: LibraryItemCreationAttributes): LibraryItemModel {
        return LibraryItemModel(
            name = creationAttributes.name,
            colorIndex = creationAttributes.colorIndex,
            libraryFolderId = creationAttributes.libraryFolderId,
        )
    }

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        oldModel: LibraryItemModel,
        updateAttributes: LibraryItemUpdateAttributes
    ): LibraryItemModel = super.applyUpdateAttributes(oldModel, updateAttributes).apply {
        name = updateAttributes.name ?: oldModel.name
        colorIndex = updateAttributes.colorIndex ?: oldModel.colorIndex
        libraryFolderId = updateAttributes.libraryFolderId ?: oldModel.libraryFolderId
        customOrder = updateAttributes.customOrder ?: oldModel.customOrder
    }

    /**
     *  @Queries
     */

    @Transaction
    @Query(
        "SELECT " +
            "library_item.id, " +
            "library_item.name, " +
            "library_item.color_index, " +
            "library_item.custom_order, " +
            "library_item.created_at, " +
            "library_item.modified_at, " +
            "CASE WHEN library_folder.deleted=1 THEN NULL ELSE library_folder.id END AS library_folder_id" +
            " FROM library_item LEFT JOIN library_folder ON library_item.library_folder_id = library_folder.id WHERE " +
            "library_item.deleted=0"
    )
    abstract override fun getAllAsFlow(): Flow<List<LibraryItem>>

    override suspend fun clean(query: SimpleSQLiteQuery): Int {
        return cleanItems()
    }

    @Transaction
    @Query(
        "DELETE FROM library_item WHERE " +
            "deleted=1 " +
            "AND (NOT EXISTS (SELECT id FROM section WHERE library_item_id = library_item.id)) " +
            "AND (NOT EXISTS (SELECT id FROM goal_description_library_item_cross_ref WHERE library_item_id = library_item.id)) " +
            "AND (datetime(SUBSTR(modified_at, 1, INSTR(modified_at, '[') - 1)) < datetime(:now, '-1 month'));"
    )
    protected abstract suspend fun cleanItems(
        now: String = database.timeProvider.now().toDatabaseInterpretableString()
    ): Int
}
