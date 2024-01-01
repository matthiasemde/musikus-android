/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
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
data class SessionModel (
    @ColumnInfo(name="break_duration_seconds") val breakDurationSeconds: Int,
    @ColumnInfo(name="rating") override var rating: Int,
    @ColumnInfo(name="comment") override var comment: String,
//    @ColumnInfo(name="profile_id", index = true) override val profileId: UUID? = null,
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
