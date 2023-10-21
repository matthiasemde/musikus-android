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

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import app.musikus.database.PTDatabase
import app.musikus.database.SoftDeleteDao
import app.musikus.database.entities.LibraryItem
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
abstract class LibraryItemDao(
    database: PTDatabase
) : SoftDeleteDao<LibraryItem>(
    tableName = "library_item",
    database = database
) {

    // Merge all LibraryItemUpdateAttributes
    override fun merge(old: LibraryItem, new: LibraryItem): LibraryItem {
        return super.merge(old, new).apply {
            name = new.name ?: old.name
            colorIndex = new.colorIndex ?: old.colorIndex
            libraryFolderId = new.libraryFolderId ?: old.libraryFolderId
            order = new.order ?: old.order
        }
    }

    @Update
    override suspend fun update(row: LibraryItem) {
        super.update(row)
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
                "library_item.deleted, " +
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