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
import de.practicetime.practicetime.database.BaseDao
import de.practicetime.practicetime.database.SectionWithLibraryItem
import de.practicetime.practicetime.database.entities.Section
import java.util.*

@Dao
abstract class SectionDao : BaseDao<Section>(tableName = "section") {


    /**
     * @Queries
     */

    @Query("SELECT * FROM section WHERE session_id=:sessionId")
    abstract suspend fun getFromSession(sessionId: UUID): List<Section>

    @Transaction
    @Query("SELECT * FROM section WHERE timestamp>=:beginTimeStamp AND timestamp<=:endTimeStamp")
    abstract suspend fun getWithLibraryItems(
        beginTimeStamp: Long,
        endTimeStamp: Long
    ): List<SectionWithLibraryItem>
}
