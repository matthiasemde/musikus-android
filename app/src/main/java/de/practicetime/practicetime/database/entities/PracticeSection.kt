package de.practicetime.practicetime.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PracticeSection (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var practice_session_id: Int?,
    val category_id: Int,
    var duration: Int?,
    val timestamp: Long,
)