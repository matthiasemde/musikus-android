package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PracticeSection (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(index = true) var practice_session_id: Int?,
    @ColumnInfo(index = true) val category_id: Long,
    var duration: Int?,
    val timestamp: Long,
)