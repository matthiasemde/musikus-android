package de.practicetime.practicetime.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Category (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var name: String,
    var colorIndex: Int,
    var archived: Boolean = false,
    val profile_id: Int = 0,
)