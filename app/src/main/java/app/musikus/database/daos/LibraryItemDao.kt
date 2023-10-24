/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.database.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import app.musikus.database.PTDatabase
import app.musikus.database.SoftDeleteDao
import app.musikus.database.SoftDeleteModelDisplayAttributes
import app.musikus.database.entities.LibraryItemModel
import app.musikus.database.entities.LibraryItemUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.util.*

data class LibraryItem(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    @ColumnInfo(name = "custom_order") val order: Int?,
    @ColumnInfo(name = "library_folder_id") val libraryFolderId: UUID?
) : SoftDeleteModelDisplayAttributes()

@Dao
abstract class LibraryItemDao(
    database: PTDatabase
) : SoftDeleteDao<
    LibraryItemModel,
    LibraryItemUpdateAttributes,
    LibraryItem
>(
    tableName = "library_item",
    database = database,
    displayAttributes = listOf("name", "color_index", "library_folder_id")
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: LibraryItemModel,
        updateAttributes: LibraryItemUpdateAttributes
    ): LibraryItemModel = super.applyUpdateAttributes(old, updateAttributes).apply{
        name = updateAttributes.name ?: old.name
        colorIndex = updateAttributes.colorIndex ?: old.colorIndex
        libraryFolderId = updateAttributes.libraryFolderId ?: old.libraryFolderId
        order = updateAttributes.order ?: old.order
    }

    /**
    *  @Queries
    */

//    @Query(
//        "SELECT * FROM library_item WHERE " +
//            "(library_item.archived=0 OR NOT :activeOnly) " +
//            "AND library_item.deleted=0"
//    )
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

    @Query(
        "DELETE FROM library_item WHERE " +
            "deleted=1 " +
            "AND (NOT EXISTS (SELECT id FROM section WHERE library_item_id = library_item.id)) " +
            "AND (NOT EXISTS (SELECT id FROM goal_description_library_item_cross_ref WHERE library_item_id = library_item.id)) "
    )
    abstract suspend fun clean()

//    @Transaction
//    @Query("SELECT * FROM library_item WHERE id=:id")
//    abstract suspend fun getWithGoalDescriptions(id: UUID): LibraryItemWithGoalDescriptions?
}