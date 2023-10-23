/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import app.musikus.database.SoftDeleteModel
import app.musikus.database.ISoftDeleteModelCreationAttributes
import app.musikus.database.SoftDeleteModelCreationAttributes
import app.musikus.database.ISoftDeleteModelUpdateAttributes
import app.musikus.database.SoftDeleteModelUpdateAttributes

private interface ISessionCreationAttributes : ISoftDeleteModelCreationAttributes {
    val breakDuration: Int
    val rating: Int
    val comment: String
}

private interface ISessionUpdateAttributes : ISoftDeleteModelUpdateAttributes {
    val rating: Int?
    val comment: String?
}

data class SessionCreationAttributes(
    override val breakDuration: Int,
    override val rating: Int,
    override val comment: String,
) : SoftDeleteModelCreationAttributes(), ISessionCreationAttributes

data class SessionUpdateAttributes(
    override val rating: Int? = null,
    override val comment: String? = null,
) : SoftDeleteModelUpdateAttributes(), ISessionUpdateAttributes

@Entity(tableName = "session")
data class Session (
    @ColumnInfo(name="break_duration") override val breakDuration: Int,
    @ColumnInfo(name="rating") override var rating: Int,
    @ColumnInfo(name="comment") override var comment: String,
//    @ColumnInfo(name="profile_id", index = true) override val profileId: UUID? = null,
) : SoftDeleteModel(), ISessionCreationAttributes, ISessionUpdateAttributes
