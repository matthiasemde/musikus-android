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
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Section(
    @ColumnInfo(name="id") override val id: UUID,
    @ColumnInfo(name="session_id") val sessionId: UUID,
    @ColumnInfo(name="library_item_id") val libraryItemId: UUID,
    @ColumnInfo(name="duration_seconds") val durationSeconds: Long,
    @ColumnInfo(name="start_timestamp") val startTimestamp: ZonedDateTime,
) : BaseModelDisplayAttributes() {

    val duration: Duration
        get() = durationSeconds.seconds

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        super.equals(other) &&
                (other is Section) &&
                (other.durationSeconds == durationSeconds)

    override fun hashCode() =
        super.hashCode() * HASH_FACTOR + durationSeconds.hashCode()
}

@Dao
abstract class SectionDao(
    private val database: MusikusDatabase
) : BaseDao<SectionModel, SectionUpdateAttributes, Section>(
    tableName = "section",
    database = database,
    displayAttributes = listOf(
        "session_id",
        "library_item_id",
        "duration_seconds",
        "start_timestamp"
    )
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
            "AND start_timestamp<:endTimestamp " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    abstract fun getInTimeframe(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
    ): Flow<List<Section>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section " +
            "WHERE start_timestamp>=:startTimestamp " +
            "AND start_timestamp<:endTimestamp " +
            "AND library_item_id IN (:itemIds) " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    protected abstract fun directGetInTimeframeForItemId(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
        itemIds: List<UUID>
    ): Flow<List<Section>>

    suspend fun getInTimeframeForItemId(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
        itemIds: List<UUID>
    ): Flow<List<Section>> {
        // Check if itemIds exist by querying for them
        database.libraryItemDao.getAsFlow(itemIds).first()

        return directGetInTimeframeForItemId(
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            itemIds = itemIds
        )
    }
}
