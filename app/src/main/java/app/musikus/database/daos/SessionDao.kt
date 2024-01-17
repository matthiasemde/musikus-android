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

    val breakDuration: Duration
        get() = breakDurationSeconds.seconds

    // necessary custom equals operator since default does not check super class properties
    override fun equals(other: Any?) =
        super.equals(other) &&
                (other is Session) &&
                (other.rating == rating) &&
                (other.comment == comment)

    override fun hashCode() =
        (super.hashCode() *
                HASH_FACTOR + rating.hashCode()) *
                HASH_FACTOR + comment.hashCode()
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
    abstract fun getAllWithSectionsWithLibraryItemsAsFlow(
    ): Flow<List<SessionWithSectionsWithLibraryItems>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM session " +
            "WHERE datetime(:startTimestamp) <= (SELECT min(datetime(start_timestamp)) FROM section WHERE section.session_id = session.id) " +
            "AND datetime(:endTimestamp) > (SELECT min(datetime(start_timestamp)) FROM section WHERE section.session_id = session.id) " +
            "AND deleted=0"
    )
    abstract fun getFromTimeframe(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime
    ): Flow<List<SessionWithSectionsWithLibraryItems>>
}