/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import de.practicetime.practicetime.database.ModelWithTimestamps

@Entity(tableName = "session")
data class Session (
    @ColumnInfo(name="break_duration") val breakDuration: Int,
    @ColumnInfo(name="rating") var rating: Int,
    @ColumnInfo(name="comment") var comment: String?,
//    @ColumnInfo(name="profile_id", index = true) val profileId: UUID? = null,
) : ModelWithTimestamps()
