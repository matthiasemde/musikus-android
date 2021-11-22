package de.practicetime.practicetime.entities;

import androidx.room.Entity

@Entity(primaryKeys = ["goalId", "categoryId"])
data class GoalCategoryCrossRef (
    val goalId: Int,
    val categoryId: Int,
)
