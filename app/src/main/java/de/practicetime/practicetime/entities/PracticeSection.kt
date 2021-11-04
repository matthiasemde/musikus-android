package de.practicetime.practicetime.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PracticeSection (
    @PrimaryKey(autoGenerate = true) val id: Int,
    var practice_session_id: Int?,
    val category_id: Int?,
    val duration: Int,
    val timestamp: Long,
)