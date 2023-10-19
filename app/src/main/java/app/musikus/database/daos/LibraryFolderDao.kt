/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.database.daos

import androidx.room.Dao
import app.musikus.database.PTDatabase
import app.musikus.database.SoftDeleteDao
import app.musikus.database.entities.LibraryFolder

@Dao
abstract class LibraryFolderDao(
    database: PTDatabase
) : SoftDeleteDao<LibraryFolder>(
    tableName = "library_folder",
    database = database
)