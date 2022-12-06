/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.database.daos

import androidx.room.Dao
import de.practicetime.practicetime.database.BaseDao
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.LibraryFolder

@Dao
abstract class LibraryFolderDao(
    database: PTDatabase
) : BaseDao<LibraryFolder>(
    tableName = "library_folder",
    database = database
) {

    /**
     * @Delete / archive
     */

//    @Transaction
//    open suspend fun deleteAndResetItems(folderId: UUID) {
//        PracticeTime.libraryItemDao.getFromFolder(folderId).forEach {
//            it.libraryFolderId = null
//            PracticeTime.libraryItemDao.update(it)
//        }
//        getAndDelete(folderId)
//    }

    /**
     *  @Queries
     */
//
//    @Query("SELECT * FROM library_folder")
//    abstract suspend fun get(): List<LibraryFolder>

//    @Transaction
//    @Query("SELECT * FROM library_folder WHERE id=:id")
//    abstract suspend fun getWithItems(id: UUID): LibraryFolderWithItems


//    @RawQuery(observedEntities = [LibraryFolder::class])
//    protected abstract fun getAsFlow(query: SupportSQLiteQuery): Flow<LibraryFolder>
//
//    open fun getAsFlow(id: UUID): Flow<LibraryFolder> {
//        return getAsFlow(SimpleSQLiteQuery("SELECT * FROM library_folder WHERE id=x'${UUIDConverter.toDBString(id)}'"))
//    }
//
//    @RawQuery(observedEntities = [LibraryFolder::class])
//    protected abstract fun getAllAsFlow(query: SupportSQLiteQuery): Flow<List<LibraryFolder>>
//
//    open fun getAllAsFlow(): Flow<List<LibraryFolder>> {
//        return getAllAsFlow(SimpleSQLiteQuery("SELECT * FROM library_folder"))
//    }
}