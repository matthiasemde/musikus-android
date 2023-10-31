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

package app.musikus.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import app.musikus.database.Nullable
import java.util.UUID

private interface ISectionCreationAttributes : IBaseModelCreationAttributes {
    val libraryItemId: Nullable<UUID>
    val duration: Int
    val timestamp: Long
}

private interface ISectionUpdateAttributes : IBaseModelUpdateAttributes {
    val duration: Int?
}

data class SectionCreationAttributes(
    override val libraryItemId: Nullable<UUID>,
    override var duration: Int,
    override val timestamp: Long,
) : BaseModelCreationAttributes(), ISectionCreationAttributes

data class SectionUpdateAttributes(
    override val duration: Int? = null,
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
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class SectionModel (
    @ColumnInfo(name="session_id", index = true) val sessionId: Nullable<UUID>,
    @ColumnInfo(name="library_item_id", index = true) override val libraryItemId: Nullable<UUID>,
    @ColumnInfo(name="duration") override var duration: Int,
    @ColumnInfo(name="timestamp") override val timestamp: Long,
) : BaseModel(), ISectionCreationAttributes, ISectionUpdateAttributes
