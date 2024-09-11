/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import app.musikus.core.data.entities.ISoftDeleteModelCreationAttributes
import app.musikus.core.data.entities.ISoftDeleteModelUpdateAttributes
import app.musikus.core.data.entities.SoftDeleteModel
import app.musikus.core.data.entities.SoftDeleteModelCreationAttributes
import app.musikus.core.data.entities.SoftDeleteModelUpdateAttributes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private interface ISessionCreationAttributes : ISoftDeleteModelCreationAttributes {
    val breakDuration: Duration
    val rating: Int
    val comment: String
}

private interface ISessionUpdateAttributes : ISoftDeleteModelUpdateAttributes {
    val rating: Int?
    val comment: String?
}

data class SessionCreationAttributes(
    override val breakDuration: Duration,
    override val rating: Int,
    override val comment: String,
) : SoftDeleteModelCreationAttributes(), ISessionCreationAttributes

data class SessionUpdateAttributes(
    override val rating: Int? = null,
    override val comment: String? = null,
) : SoftDeleteModelUpdateAttributes(), ISessionUpdateAttributes

@Entity(tableName = "session")
data class SessionModel(
    @ColumnInfo(name = "break_duration_seconds") val breakDurationSeconds: Int,
    @ColumnInfo(name = "rating") override var rating: Int,
    @ColumnInfo(name = "comment") override var comment: String,
) : SoftDeleteModel(), ISessionCreationAttributes, ISessionUpdateAttributes {

    @get:Ignore
    override val breakDuration: Duration
        get() = breakDurationSeconds.seconds

    @Ignore
    constructor(
        breakDuration: Duration,
        rating: Int,
        comment: String,
    ) : this(
        breakDurationSeconds = breakDuration.inWholeSeconds.toInt(),
        rating = rating,
        comment = comment,
    )
}
