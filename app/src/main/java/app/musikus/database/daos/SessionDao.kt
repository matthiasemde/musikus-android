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

@Dao
abstract class SessionDao(
    private val database : PTDatabase
) : SoftDeleteDao<
    Session,
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
        old: Session,
        updateAttributes: SessionUpdateAttributes
    ): Session = super.applyUpdateAttributes(old, updateAttributes).apply {
        rating = updateAttributes.rating ?: old.rating
        comment = updateAttributes.comment ?: old.comment
    }

    /**
     * @Insert
      */

    @Transaction
    open suspend fun insert(
        sessionWithSections: SessionWithSections,
    ) {
        insert(sessionWithSections.session)
        database.sectionDao.insert(sessionWithSections.sections.map {
            it.copy(sessionId = sessionWithSections.session.id) }
        )
    }

    /**
     * @Queries
     */

    @Transaction
    @Query("SELECT * FROM session WHERE id=:sessionId AND deleted=0")
    abstract fun getWithSectionsWithLibraryItemsAsFlow(
        sessionId: UUID
    ): Flow<SessionWithSectionsWithLibraryItems>

    @Transaction
    @Query("SELECT * FROM session WHERE deleted=0")
    abstract fun getAllWithSectionsWithLibraryItemsAsFlow(
    ): Flow<List<SessionWithSectionsWithLibraryItems>>
}
