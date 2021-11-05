package de.practicetime.practicetime.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PracticeSession (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val break_duration: Int,
    val rating: Int,
    val comment: String?,
    val profile_id: Int
)