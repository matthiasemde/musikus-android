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
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.database.entities.SessionModel
import app.musikus.database.entities.SessionUpdateAttributes
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes
import app.musikus.database.toDatabaseInterpretableString
import app.musikus.utils.Timeframe
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Session(
    @ColumnInfo(name="id") override val id: UUID,
    @ColumnInfo(name="created_at") override val createdAt: ZonedDateTime,
    @ColumnInfo(name="modified_at") override val modifiedAt: ZonedDateTime,
    @ColumnInfo(name="break_duration_seconds") val breakDurationSeconds: Long,
    @ColumnInfo(name="rating") val rating: Int,
    @ColumnInfo(name="comment") val comment: String?,
) : SoftDeleteModelDisplayAttributes() {

    override fun toString(): String {
        return super.toString() +
            "\tbreakDuration:\t\t\t$breakDuration\n" +
            "\trating:\t\t\t\t\t$rating\n" +
            "\tcomment:\t\t\t\t$comment\n"
    }

    val breakDuration: Duration
        get() = breakDurationSeconds.seconds
}
@Dao
abstract class SessionDao(
    private val database : MusikusDatabase,
) : SoftDeleteDao<
        SessionModel,
        SessionCreationAttributes,
        SessionUpdateAttributes,
        Session
        >(
    tableName = "session",
    database = database,
    displayAttributes = listOf("break_duration_seconds", "rating", "comment")
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        oldModel: SessionModel,
        updateAttributes: SessionUpdateAttributes
    ): SessionModel = super.applyUpdateAttributes(oldModel, updateAttributes).apply {
        rating = updateAttributes.rating ?: oldModel.rating
        comment = updateAttributes.comment ?: oldModel.comment
    }

    /**
     * @Insert
     */

    override fun createModel(creationAttributes: SessionCreationAttributes): SessionModel {
        return SessionModel(
            breakDuration = creationAttributes.breakDuration,
            rating = creationAttributes.rating,
            comment = creationAttributes.comment
        )
    }

    override suspend fun insert(creationAttributes: SessionCreationAttributes): UUID {
        throw NotImplementedError("Use insert(sessionCreationAttributes, sectionCreationAttributes) instead")
    }

    override suspend fun insert(creationAttributes: List<SessionCreationAttributes>): List<UUID> {
        throw NotImplementedError("Use insert(sessionCreationAttributes, sectionCreationAttributes) instead")
    }

    @Transaction
    open suspend fun insert(
        sessionCreationAttributes: SessionCreationAttributes,
        sectionCreationAttributes: List<SectionCreationAttributes>
    ): Pair<UUID, List<UUID>> {
        if(sectionCreationAttributes.isEmpty()) {
            throw IllegalArgumentException("Each session must include at least one section")
        }

        val sessionId = super.insert(listOf(sessionCreationAttributes)).single() // insert of single session would call the overridden insert method

        val sectionIds = database.sectionDao.insert(sectionCreationAttributes.onEach {
            it.sessionId = sessionId
        })

        return Pair(sessionId, sectionIds)
    }

    /**
     * @Queries
     */

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM session WHERE id=:sessionId AND deleted=0")
    protected abstract suspend fun directGetWithSectionsWithLibraryItems(
        sessionId: UUID
    ): SessionWithSectionsWithLibraryItems?

    suspend fun getWithSectionsWithLibraryItems(
        sessionId: UUID
    ): SessionWithSectionsWithLibraryItems {
        return directGetWithSectionsWithLibraryItems(sessionId)
            ?: throw IllegalArgumentException("Session with id $sessionId not found")
    }


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM session WHERE deleted=0")
    abstract fun getAllWithSectionsWithLibraryItemsAsFlow()
        : Flow<List<SessionWithSectionsWithLibraryItems>>


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
    SELECT session.*
    FROM
        session
        JOIN (
            SELECT
                session_id,
                min(datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))) AS start_timestamp
            FROM section
            GROUP BY session_id
        ) AS ids_with_start_timestamp
            ON ids_with_start_timestamp.session_id = session.id 
    WHERE deleted=0 
    ORDER BY ids_with_start_timestamp.start_timestamp DESC 
    """)
//         LIMIT 50
    abstract fun getOrderedWithSectionsWithLibraryItems()
            : Flow<List<SessionWithSectionsWithLibraryItems>>


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT session.*
        FROM
            session
            JOIN (
                SELECT
                    session_id,
                    min(datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))) AS start_timestamp
                FROM section
                GROUP BY session_id
            ) AS ids_with_start_timestamp
                ON ids_with_start_timestamp.session_id = session.id
        
        WHERE 
            datetime(:startTimestamp) <= start_timestamp AND
            datetime(:endTimestamp) > start_timestamp AND
            deleted=0
    """)
    protected abstract fun directGetFromTimeframe(
        startTimestamp: String,
        endTimestamp: String
    ): Flow<List<SessionWithSectionsWithLibraryItems>>

    fun getFromTimeframe(
        timeframe: Timeframe
    ): Flow<List<SessionWithSectionsWithLibraryItems>> {
        return directGetFromTimeframe(
            startTimestamp = timeframe.first.toDatabaseInterpretableString(),
            endTimestamp = timeframe.second.toDatabaseInterpretableString()
        )
    }
}

class InvalidSessionException(message: String) : Exception(message)
