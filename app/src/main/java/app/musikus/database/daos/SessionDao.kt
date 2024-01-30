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

import android.database.Cursor
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.musikus.database.MusikusDatabase
import app.musikus.database.SectionWithLibraryItem
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.UUIDConverter
import app.musikus.database.ZonedDateTimeConverter
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.database.entities.SessionModel
import app.musikus.database.entities.SessionUpdateAttributes
import app.musikus.database.entities.SoftDeleteModelDisplayAttributes
import app.musikus.database.toDatabaseInterpretableString
import app.musikus.utils.TimeProvider
import app.musikus.utils.Timeframe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    @Query(
        "SELECT session.* FROM session " +
            "JOIN (" +
                "SELECT session_id, min(datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))) AS start_timestamp FROM section " +
                "GROUP BY session_id" +
            ") AS ordered_ids_with_start_timestamp ON ordered_ids_with_start_timestamp.session_id = session.id " +
            "WHERE deleted=0 " +
            "ORDER BY ordered_ids_with_start_timestamp.start_timestamp DESC " +
//            "LIMIT 50" +
            ""
    )
    abstract fun getOrderedWithSectionsWithLibraryItems()
        : Flow<List<SessionWithSectionsWithLibraryItems>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT session.* FROM session " +
            "JOIN (" +
                "SELECT session_id, min(start_timestamp_seconds) AS start_timestamp FROM section " +
                "GROUP BY session_id" +
            ") AS ordered_ids_with_start_timestamp ON ordered_ids_with_start_timestamp.session_id = session.id " +
            "WHERE deleted=0 " +
            "ORDER BY ordered_ids_with_start_timestamp.start_timestamp DESC " +
//            "LIMIT 50" +
            ""
    )
    abstract fun getOrderedWithSectionsWithLibraryItemsUsingEpochSeconds()
        : Flow<List<SessionWithSectionsWithLibraryItems>>


    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM session " +
            "WHERE deleted=0 " +
//            "ORDER BY (SELECT min(datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))) FROM section WHERE section.session_id = session.id) DESC"
            "ORDER BY (SELECT min(datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))) FROM section WHERE section.session_id = session.id) DESC " +
//             "LIMIT 10" +
            ""
    )
    abstract fun getOrdered()
        : Flow<List<Session>>


    @OptIn(ExperimentalCoroutinesApi::class)
    fun getOrderedWithOrderedSections() : Flow<List<SessionWithSectionsWithLibraryItems>> {
        return getOrdered().flatMapLatest { sessions ->
            combine(sessions.map { session ->
                database.sectionDao.getOrderedForSession(session.id).map { sections ->
                    SessionWithSectionsWithLibraryItems(
                        session = session,
                        sections = sections
                    )
                }
            }) {
                it.toList()
            }
        }
    }

    @Query(
        "SELECT " +
                "session.id AS sessionId, " +
                "session.created_at AS sessionCreatedAt, " +
                "session.modified_at AS sessionModifiedAt, " +
                "session.break_duration_seconds, " +
                "session.rating, " +
                "session.comment, " +
                "section.id AS sectionId, " +
                "section.duration_seconds, " +
                "section.start_timestamp, " +
                "library_item.id AS libraryItemId, " +
                "library_item.name, " +
                "library_item.color_index " +
                "FROM session " +
            "JOIN section ON section.session_id = session.id " +
            "JOIN library_item ON section.library_item_id = library_item.id " +
            "WHERE session.deleted = 0 " +
//            "ORDER BY start_timestamp_seconds DESC"
            "ORDER BY datetime(SUBSTR(section.start_timestamp, 1, INSTR(section.start_timestamp, '[') - 1)) DESC"
    )
    protected abstract fun getCursorForAllSessionsWithSectionsWithLibraryItems(
    ) : Cursor


    fun getAllWithSectionsWithLibraryItems(
    ): List<SessionWithSectionsWithLibraryItems> {
        val uuidConverter = UUIDConverter()
        val zonedDateTimeConverter = ZonedDateTimeConverter()

        return getCursorForAllSessionsWithSectionsWithLibraryItems().let { cursor ->

            val columnIndexSessionId = cursor.getColumnIndexOrThrow("sessionId")
            val columnIndexSessionCreatedAt = cursor.getColumnIndexOrThrow("sessionCreatedAt")
            val columnIndexSessionModifiedAt = cursor.getColumnIndexOrThrow("sessionModifiedAt")
            val columnIndexSessionBreakDurationSeconds = cursor.getColumnIndexOrThrow("break_duration_seconds")

            val columnIndexSectionId = cursor.getColumnIndexOrThrow("sectionId")
            val columnIndexSectionStartTimestamp = cursor.getColumnIndexOrThrow("start_timestamp")

            val sessions = mutableListOf<SessionWithSectionsWithLibraryItems>()

            val dummyId = UUIDConverter.fromInt(1)

            if (cursor.moveToFirst()) {
                var session : Session? = null
                val sections = mutableListOf<SectionWithLibraryItem>()
                do {
                    val sessionId = cursor.getBlob(columnIndexSessionId).let {
                        uuidConverter.fromByte(it)
                    } ?: throw InvalidSessionException("Session id must not be null")

                    if(session == null || (session.id != sessionId)) {
                        if(session != null) {
                            sessions.add(
                                SessionWithSectionsWithLibraryItems(
                                    session = session,
                                    sections = sections
                                )
                            )
                            sections.clear()
                        }

                        session = Session(
                            id = sessionId,
//                            createdAt = TimeProvider.uninitializedDateTime,
                            createdAt = cursor.getString(columnIndexSessionCreatedAt).let {
                                zonedDateTimeConverter.toZonedDateTime(it)
                            } ?: throw InvalidSessionException("Session createdAt must not be null"),
//                            modifiedAt = TimeProvider.uninitializedDateTime,
                            modifiedAt = cursor.getString(columnIndexSessionModifiedAt).let {
                                zonedDateTimeConverter.toZonedDateTime(it)
                            } ?: throw InvalidSessionException("Session modifiedAt must not be null"),
//                            breakDurationSeconds = 0L,
                            breakDurationSeconds = cursor.getLong(columnIndexSessionBreakDurationSeconds),
                            rating = 1,
                            comment = ""
                        )
                    }

                    sections.add(
                        SectionWithLibraryItem(
                            section = Section(
//                                id = dummyId,
                                id = cursor.getBlob(columnIndexSectionId).let {
                                    uuidConverter.fromByte(it)
                                } ?: throw InvalidSessionException("Section id must not be null"),
//                                startTimestamp = TimeProvider.uninitializedDateTime,
                                startTimestamp = cursor.getString(columnIndexSectionStartTimestamp).let {
                                    zonedDateTimeConverter.toZonedDateTime(it)
                                } ?: throw InvalidSessionException("Section startTimestamp must not be null"),
                                durationSeconds = 100,
                                libraryItemId = dummyId,
                                sessionId = sessionId
                            ),
                            libraryItem = LibraryItem(
                                id = dummyId,
                                createdAt = TimeProvider.uninitializedDateTime,
                                modifiedAt = TimeProvider.uninitializedDateTime,
                                name = "Test",
                                colorIndex = 3,
                                customOrder = null,
                                libraryFolderId = null
                            )
                        )
                    )
                } while (cursor.moveToNext())

                // Add the last session if it exists
                if(session != null) {
                    sessions.add(
                        SessionWithSectionsWithLibraryItems(
                            session = session,
                            sections = sections
                        )
                    )
                }
            }

            return@let sessions
        }
    }


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM session " +
            "WHERE datetime(:startTimestamp) <= (SELECT min(datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))) FROM section WHERE section.session_id = session.id) " +
            "AND datetime(:endTimestamp) > (SELECT min(datetime(SUBSTR(start_timestamp, 1, INSTR(start_timestamp, '[') - 1))) FROM section WHERE section.session_id = session.id) " +
            "AND deleted=0"
    )
    abstract fun directGetFromTimeframe(
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
