/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import de.practicetime.practicetime.database.BaseModel
import java.util.*

@Entity(tableName = "section")
data class Section (
    @ColumnInfo(name="session_id", index = true) var sessionId: UUID?,
    @ColumnInfo(name="library_item_id", index = true) val libraryItemId: UUID,
    @ColumnInfo(name="duration") var duration: Int?,
    @ColumnInfo(name="timestamp") val timestamp: Long,
) : BaseModel()
