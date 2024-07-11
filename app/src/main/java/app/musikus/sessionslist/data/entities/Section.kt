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

package app.musikus.sessionslist.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import app.musikus.core.data.entities.BaseModel
import app.musikus.core.data.entities.BaseModelCreationAttributes
import app.musikus.core.data.entities.BaseModelUpdateAttributes
import app.musikus.core.data.entities.IBaseModelCreationAttributes
import app.musikus.core.data.entities.IBaseModelUpdateAttributes
import app.musikus.core.data.UUIDConverter
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private interface ISectionCreationAttributes : IBaseModelCreationAttributes {
    var sessionId: UUID
    var libraryItemId: UUID
    var duration: Duration
    var startTimestamp: ZonedDateTime
}

private interface ISectionUpdateAttributes : IBaseModelUpdateAttributes {
    val duration: Duration?
}

data class SectionCreationAttributes(
    override var sessionId: UUID = UUIDConverter.deadBeef,
    override var libraryItemId: UUID,
    override var duration: Duration,
    override var startTimestamp: ZonedDateTime,
) : BaseModelCreationAttributes(), ISectionCreationAttributes

data class SectionUpdateAttributes(
    override val duration: Duration? = null,
) : BaseModelUpdateAttributes(), ISectionUpdateAttributes

@Entity(
    tableName = "section",
    foreignKeys = [
        ForeignKey(
            entity = SessionModel::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LibraryItemModel::class,
            parentColumns = ["id"],
            childColumns = ["library_item_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class SectionModel (
    @ColumnInfo(name="session_id", index = true) override var sessionId: UUID,
    @ColumnInfo(name="library_item_id", index = true) override var libraryItemId: UUID,
    @ColumnInfo(name="duration_seconds") var durationSeconds: Int,
    @ColumnInfo(name="start_timestamp") override var startTimestamp: ZonedDateTime,
) : BaseModel(), ISectionCreationAttributes, ISectionUpdateAttributes {

    @get:Ignore
    override var duration: Duration
        get() = durationSeconds.seconds
        set(value) { durationSeconds = value.inWholeSeconds.toInt() }

    @Ignore
    constructor(
        sessionId: UUID,
        libraryItemId: UUID,
        duration: Duration,
        startTimestamp: ZonedDateTime,
    ) : this(
        sessionId = sessionId,
        libraryItemId = libraryItemId,
        durationSeconds = duration.inWholeSeconds.toInt(),
        startTimestamp = startTimestamp
    )
}