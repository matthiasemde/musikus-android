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

package app.musikus.database.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.musikus.database.BaseDao
import app.musikus.database.BaseModelDisplayAttributes
import app.musikus.database.PTDatabase
import app.musikus.database.SectionWithLibraryItem
import app.musikus.database.entities.SectionModel
import app.musikus.database.entities.SectionUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.util.*

data class Section(
    @ColumnInfo(name = "session_id") val sessionId: UUID,
    @ColumnInfo(name = "library_item_id") val libraryItemId: UUID,
    @ColumnInfo(name = "duration") val duration: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
) : BaseModelDisplayAttributes()

@Dao
abstract class SectionDao(
    database: PTDatabase
) : BaseDao<
    SectionModel,
    SectionUpdateAttributes,
    Section
>(
    tableName = "section",
    database = database,
    displayAttributes = listOf("session_id", "library_item_id", "duration", "timestamp")
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: SectionModel,
        updateAttributes: SectionUpdateAttributes
    ): SectionModel = super.applyUpdateAttributes(old, updateAttributes).apply {
        duration = updateAttributes.duration ?: old.duration
    }

    /**
     * @Queries
     */

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section WHERE session_id=:sessionId")
    abstract suspend fun getFromSession(sessionId: UUID): List<Section>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section WHERE timestamp>=:startTimeStamp AND timestamp<=:endTimeStamp")
    abstract suspend fun getWithLibraryItems(
        startTimeStamp: Long,
        endTimeStamp: Long
    ): List<SectionWithLibraryItem>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section WHERE timestamp>=:startTimeStamp AND timestamp<=:endTimeStamp")
    abstract fun get(
        startTimeStamp: Long,
        endTimeStamp: Long,
    ): Flow<List<Section>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section WHERE timestamp>=:startTimeStamp AND timestamp<=:endTimeStamp AND library_item_id IN (:itemIds)")
    abstract fun get(
        startTimeStamp: Long,
        endTimeStamp: Long,
        itemIds: List<UUID>
    ): Flow<List<Section>>
}
