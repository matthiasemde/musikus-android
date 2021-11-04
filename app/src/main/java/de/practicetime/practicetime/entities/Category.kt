package de.practicetime.practicetime.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Category (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val color: Int,
    val archived: Boolean,
    val profile_id: Int,
)