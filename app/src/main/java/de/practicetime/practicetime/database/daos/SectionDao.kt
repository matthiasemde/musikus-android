/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import de.practicetime.practicetime.database.BaseDao
import de.practicetime.practicetime.database.entities.Section
import de.practicetime.practicetime.database.entities.SectionWithCategory

@Dao
abstract class SectionDao : BaseDao<Section>(tableName = "section") {


    /**
     * @Queries
     */

    @Query("SELECT * FROM section WHERE session_id=:sessionId")
    abstract suspend fun getFromSession(sessionId: Long): List<Section>

    @Transaction
    @Query("SELECT * FROM section WHERE timestamp>=:beginTimeStamp AND timestamp<=:endTimeStamp")
    abstract suspend fun getWithCategories(
        beginTimeStamp: Long,
        endTimeStamp: Long
    ): List<SectionWithCategory>
}
