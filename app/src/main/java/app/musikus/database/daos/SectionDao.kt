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
import app.musikus.database.MusikusDatabase
import app.musikus.database.entities.BaseModelDisplayAttributes
import app.musikus.database.entities.SectionModel
import app.musikus.database.entities.SectionUpdateAttributes
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

data class Section(
    @ColumnInfo(name = "session_id") val sessionId: UUID,
    @ColumnInfo(name = "library_item_id") val libraryItemId: UUID,
    @ColumnInfo(name = "duration") val duration: Int,
    @ColumnInfo(name = "start_timestamp") val startTimestamp: ZonedDateTime,
) : BaseModelDisplayAttributes() {

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        super.equals(other) &&
                (other is Section) &&
                (other.duration == duration)

    override fun hashCode() =
        super.hashCode() * HASH_FACTOR + duration.hashCode()
}

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

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section " +
            "WHERE start_timestamp>=:startTimestamp " +
            "AND start_timestamp<=:endTimestamp " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    abstract fun get(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
    ): Flow<List<Section>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section " +
            "WHERE start_timestamp>=:startTimestamp " +
            "AND start_timestamp<=:endTimestamp " +
            "AND library_item_id IN (:itemIds) " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    abstract fun get(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
        itemIds: List<UUID>
    ): Flow<List<Section>>
}
