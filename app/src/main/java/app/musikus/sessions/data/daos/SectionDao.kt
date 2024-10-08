/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.sessions.data.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import app.musikus.core.data.MusikusDatabase
import app.musikus.core.data.daos.BaseDao
import app.musikus.core.data.entities.BaseModelDisplayAttributes
import app.musikus.core.data.toDatabaseInterpretableString
import app.musikus.sessions.data.entities.SectionCreationAttributes
import app.musikus.sessions.data.entities.SectionModel
import app.musikus.sessions.data.entities.SectionUpdateAttributes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Section(
    @ColumnInfo(name = "id") override val id: UUID,
    @ColumnInfo(name = "session_id") val sessionId: UUID,
    @ColumnInfo(name = "library_item_id") val libraryItemId: UUID,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long,
    @ColumnInfo(name = "start_timestamp") val startTimestamp: ZonedDateTime,
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

        if (!database.sessionDao.exists(sessionId)) {
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
    @Query(
        "SELECT * FROM section " +
            "WHERE datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))>=datetime(:startTimestamp) " +
            "AND datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))<datetime(:endTimestamp) " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    protected abstract fun directGetInTimeframe(
        startTimestamp: String,
        endTimestamp: String,
    ): Flow<List<Section>>

    fun getInTimeframe(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime
    ): Flow<List<Section>> {
        return directGetInTimeframe(
            startTimestamp = startTimestamp.toDatabaseInterpretableString(),
            endTimestamp = endTimestamp.toDatabaseInterpretableString()
        )
    }

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM section " +
            "WHERE datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))>=datetime(:startTimestamp) " +
            "AND datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))<datetime(:endTimestamp) " +
            "AND library_item_id IN (:itemIds) " +
            "AND session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")"
    )
    protected abstract fun directGetInTimeframeForItemId(
        startTimestamp: String,
        endTimestamp: String,
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
            startTimestamp = startTimestamp.toDatabaseInterpretableString(),
            endTimestamp = endTimestamp.toDatabaseInterpretableString(),
            itemIds = itemIds
        )
    }

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * " +
            "FROM section " +
            "WHERE section.library_item_id IN (:itemIds) " + // filter by itemIds
            "AND section.session_id IN (" + // filter non-deleted sessions
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ")" +
            "AND NOT EXISTS (" + // filter out all sections that have a later section for the same item
            "SELECT 1 FROM section AS s " +
            "WHERE s.library_item_id = section.library_item_id " +
            "AND s.session_id IN (" +
            "SELECT id FROM session " +
            "WHERE deleted=0 " +
            ") " +
            "AND datetime(SUBSTR(s.start_timestamp, 1, INSTR(s.start_timestamp, '[') - 1)) > datetime(SUBSTR(section.start_timestamp, 1, INSTR(section.start_timestamp, '[') - 1)) " +
            ")"
    )
    abstract fun getLatestForItems(itemIds: List<UUID>): Flow<List<Section>>
}

class InvalidSectionException(message: String) : Exception(message)
