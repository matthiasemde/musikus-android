/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.database.daos

import androidx.room.Dao
import androidx.room.Transaction
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.database.BaseDao
import de.practicetime.practicetime.database.entities.LibraryFolder
import java.util.*

@Dao
abstract class LibraryFolderDao : BaseDao<LibraryFolder>(tableName = "library_folder") {

    /**
     * @Delete / archive
     */

    @Transaction
    open suspend fun deleteAndResetItems(folderId: UUID) {
        PracticeTime.libraryItemDao.getFromFolder(folderId).forEach {
            it.libraryFolderId = null
            PracticeTime.libraryItemDao.update(it)
        }
        getAndDelete(folderId)
    }

    /**
     *  @Queries
     */
//
//    @Query("SELECT * FROM library_folder")
//    abstract suspend fun get(): List<LibraryFolder>

//    @Transaction
//    @Query("SELECT * FROM library_folder WHERE id=:id")
//    abstract suspend fun getWithItems(id: UUID): LibraryFolderWithItems
}