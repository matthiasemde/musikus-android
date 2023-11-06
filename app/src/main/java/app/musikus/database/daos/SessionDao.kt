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

import androidx.room.*
import app.musikus.database.*
import app.musikus.database.entities.*
import kotlinx.coroutines.flow.Flow
import java.util.*

data class Session(
    @ColumnInfo(name = "break_duration") val breakDuration: Int,
    @ColumnInfo(name = "rating") val rating: Int,
    @ColumnInfo(name = "comment") val comment: String?,
) : SoftDeleteModelDisplayAttributes()
@Dao
abstract class SessionDao(
    private val database : MusikusDatabase
) : SoftDeleteDao<
        SessionModel,
        SessionUpdateAttributes,
        Session
        >(
    tableName = "session",
    database = database,
    displayAttributes = Session::class.java.fields.map { it.name }
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
                timestamp = it.timestamp
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
            "WHERE :startTimestamp < (SELECT min(timestamp) FROM section WHERE section.session_id = session.id) " +
            "AND :endTimestamp > (SELECT min(timestamp) FROM section WHERE section.session_id = session.id) " +
            "AND deleted=0"
    )
    abstract fun get(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<SessionWithSectionsWithLibraryItems>>
}
