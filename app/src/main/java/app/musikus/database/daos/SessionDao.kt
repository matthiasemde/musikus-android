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
import app.musikus.database.Nullable
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SectionModel
import app.musikus.database.entities.SessionModel
import app.musikus.database.entities.SessionUpdateAttributes
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

data class Session(
    @ColumnInfo(name = "id") override val id: UUID,
    @ColumnInfo(name = "created_at") override val createdAt: ZonedDateTime,
    @ColumnInfo(name = "modified_at") override val modifiedAt: ZonedDateTime,
    @ColumnInfo(name = "break_duration") val breakDuration: Int,
    @ColumnInfo(name = "rating") val rating: Int,
    @ColumnInfo(name = "comment") val comment: String?,
) : SoftDeleteModelDisplayAttributes() {

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
        SessionUpdateAttributes,
        Session
        >(
    tableName = "session",
    database = database,
    displayAttributes = listOf("break_duration", "rating", "comment")
) {

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: SessionModel,
        updateAttributes: SessionUpdateAttributes
    ): SessionModel = super.applyUpdateAttributes(old, updateAttributes).apply {
        rating = updateAttributes.rating ?: old.rating
        comment = updateAttributes.comment ?: old.comment
    }

    /**
     * @Insert
      */


    @Transaction
    open suspend fun insert(
        session: SessionModel,
        sectionCreationAttributes: List<SectionCreationAttributes>
    ) {
        insert(session)
        database.sectionDao.insert(sectionCreationAttributes.map {
            SectionModel(
                sessionId = Nullable(session.id),
                libraryItemId = it.libraryItemId,
                duration = it.duration,
                startTimestamp = it.startTimestamp
            )
        })
    }

    /**
     * @Queries
     */

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM session WHERE id=:sessionId AND deleted=0")
    abstract fun getWithSectionsWithLibraryItemsAsFlow(
        sessionId: UUID
    ): Flow<SessionWithSectionsWithLibraryItems>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM session WHERE deleted=0")
    abstract fun getAllWithSectionsWithLibraryItemsAsFlow(
    ): Flow<List<SessionWithSectionsWithLibraryItems>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM session " +
            "WHERE datetime(:startTimestamp) < (SELECT min(datetime(start_timestamp)) FROM section WHERE section.session_id = session.id) " +
            "AND datetime(:endTimestamp) > (SELECT min(datetime(start_timestamp)) FROM section WHERE section.session_id = session.id) " +
            "AND deleted=0"
    )
    abstract fun get(
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime
    ): Flow<List<SessionWithSectionsWithLibraryItems>>
}
