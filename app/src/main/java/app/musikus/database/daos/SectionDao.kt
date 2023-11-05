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
import app.musikus.database.MusikusDatabase
import app.musikus.database.SectionWithLibraryItem
import app.musikus.database.entities.BaseModelDisplayAttributes
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
    database: MusikusDatabase
) : BaseDao<SectionModel, SectionUpdateAttributes, Section>(
    tableName = "section",
    database = database,
    displayAttributes = Section::class.java.declaredFields.map { it.name }
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
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section " +
            "WHERE timestamp>=:startTimeStamp " +
            "AND timestamp<=:endTimeStamp " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    abstract suspend fun getWithLibraryItems(
        startTimeStamp: Long,
        endTimeStamp: Long
    ): List<SectionWithLibraryItem>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section " +
            "WHERE timestamp>=:startTimeStamp " +
            "AND timestamp<=:endTimeStamp " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    abstract fun get(
        startTimeStamp: Long,
        endTimeStamp: Long,
    ): Flow<List<Section>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section " +
            "WHERE timestamp>=:startTimeStamp " +
            "AND timestamp<=:endTimeStamp " +
            "AND library_item_id IN (:itemIds) " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    abstract fun get(
        startTimeStamp: Long,
        endTimeStamp: Long,
        itemIds: List<UUID>
    ): Flow<List<Section>>
}
