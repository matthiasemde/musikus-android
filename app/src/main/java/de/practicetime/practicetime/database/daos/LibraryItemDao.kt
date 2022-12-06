/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import de.practicetime.practicetime.PracticeTime.Companion.ioThread
import de.practicetime.practicetime.database.BaseDao
import de.practicetime.practicetime.database.LibraryItemWithGoalDescriptions
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.LibraryItem
import java.util.*

@Dao
abstract class LibraryItemDao(
    database: PTDatabase
) : BaseDao<LibraryItem>(
    tableName = "library_item",
    database = database
) {

    /**
     * @Delete / archive
     */

    @Transaction
    open fun archive(libraryItemId: UUID){
        ioThread {
//            val libraryItem = getAsFlow(libraryItemId).value
//            libraryItem.archived = true
//            update(libraryItem)
        }
    }


    /**
    *  @Queries
    */

    @Query("SELECT * FROM library_item WHERE archived=0 OR NOT :activeOnly")
    abstract suspend fun get(activeOnly: Boolean = false): List<LibraryItem>

    @Query("SELECT * FROM library_item WHERE library_folder_id=:libraryFolderId")
    abstract suspend fun getFromFolder(libraryFolderId: UUID): List<LibraryItem>

    @Transaction
    @Query("SELECT * FROM library_item WHERE id=:id")
    abstract suspend fun getWithGoalDescriptions(id: UUID): LibraryItemWithGoalDescriptions?


//    @RawQuery(observedEntities = [LibraryItem::class])
//    protected abstract fun getAsFlow(query: SupportSQLiteQuery): Flow<LibraryItem>
//
//    open fun getAsFlow(id: UUID): Flow<LibraryItem> {
//        return getAsFlow(SimpleSQLiteQuery("SELECT * FROM library_item WHERE id=x'${UUIDConverter.toDBString(id)}'"))
//    }
//
//    @RawQuery(observedEntities = [LibraryItem::class])
//            protected abstract fun getAllAsFlow(query: SupportSQLiteQuery): Flow<List<LibraryItem>>
//
//            open fun getAllAsFlow(): Flow<List<LibraryItem>> {
//        return getAllAsFlow(SimpleSQLiteQuery("SELECT * FROM library_item"))
//    }
}