package de.practicetime.practicetime.database.entities;

import androidx.room.Entity

@Entity(primaryKeys = ["goalDescriptionId", "categoryId"])
data class GoalDescriptionCategoryCrossRef (
    val goalDescriptionId: Int,
    val categoryId: Int,
)
