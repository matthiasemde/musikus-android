package de.practicetime.practicetime.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Category (
    @PrimaryKey(autoGenerate = true) val id: Int,
    var name: String,
    var colorIndex: Int,
    var archived: Boolean,
    val profile_id: Int,
)