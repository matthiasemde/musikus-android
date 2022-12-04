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

package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import de.practicetime.practicetime.database.BaseModel
import java.util.*

@Entity(
    tableName = "section",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LibraryItem::class,
            parentColumns = ["id"],
            childColumns = ["library_item_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class Section (
    @ColumnInfo(name="session_id", index = true) var sessionId: UUID?,
    @ColumnInfo(name="library_item_id", index = true) val libraryItemId: UUID,
    @ColumnInfo(name="duration") var duration: Int?,
    @ColumnInfo(name="timestamp") val timestamp: Long,
) : BaseModel()
