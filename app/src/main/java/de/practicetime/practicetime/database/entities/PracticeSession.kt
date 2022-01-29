package de.practicetime.practicetime.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PracticeSession (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val break_duration: Int,
    var rating: Int,
    var comment: String?,
    val profile_id: Int = 0,
)