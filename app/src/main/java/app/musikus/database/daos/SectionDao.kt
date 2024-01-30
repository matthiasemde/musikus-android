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
import app.musikus.database.SectionWithLibraryItem
import app.musikus.database.entities.BaseModelDisplayAttributes
import app.musikus.database.entities.SectionCreationAttributes
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

    override fun toString(): String {
        return super.toString() +
            "\tsessionId:\t\t\t\t$sessionId\n" +
            "\tlibraryItemId:\t\t\t$libraryItemId\n" +
            "\tdurationSeconds:\t\t$durationSeconds\n" +
            "\tstartTimestamp:\t\t\t$startTimestamp\n"
    }

    val duration: Duration
        get() = durationSeconds.seconds
}

@Dao
abstract class SectionDao(
    private val database: MusikusDatabase
) : BaseDao<
        SectionModel,
        SectionCreationAttributes,
        SectionUpdateAttributes,
        Section
        >(
    tableName = "section",
    database = database,
    displayAttributes = listOf(
        "session_id",
        "library_item_id",
        "duration_seconds",
        "start_timestamp"
    ),
    dependencies = listOf("session")
) {

    /**
     * @Insert
     */

    override fun createModel(creationAttributes: SectionCreationAttributes): SectionModel {
        return SectionModel(
            sessionId = creationAttributes.sessionId,
            libraryItemId = creationAttributes.libraryItemId,
            duration = creationAttributes.duration,
            startTimestamp = creationAttributes.startTimestamp
        )
    }

    override suspend fun insert(creationAttributes: SectionCreationAttributes): UUID {
        throw NotImplementedError("Sections are inserted only in conjunction with their session")
    }

    // this method should only be called from SessionDao
    override suspend fun insert(
        creationAttributes: List<SectionCreationAttributes>
    ): List<UUID> {
        val sessionId = creationAttributes.first().sessionId

        if(!database.sessionDao.exists(sessionId)) {
            throw IllegalArgumentException("Could not insert sections for non-existent session $sessionId")
        }

        return super.insert(creationAttributes)
    }

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        oldModel: SectionModel,
        updateAttributes: SectionUpdateAttributes
    ): SectionModel = super.applyUpdateAttributes(oldModel, updateAttributes).apply {
        duration = updateAttributes.duration ?: oldModel.duration
    }

    /**
     * @Delete
     */

    override suspend fun delete(id: UUID) {
        throw NotImplementedError("Sections are automatically deleted when their session is deleted")
    }

    override suspend fun delete(ids: List<UUID>) {
        throw NotImplementedError("Sections are automatically deleted when their session is deleted")
    }

    /**
     * @Queries
     */

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section WHERE session_id=:sessionId")
    abstract fun getForSession(sessionId: UUID): List<Section>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM section WHERE session_id=:sessionId ORDER BY datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1)) DESC")
    abstract fun getOrderedForSession(sessionId: UUID): Flow<List<SectionWithLibraryItem>>

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

class InvalidSectionException(message: String) : Exception(message)
