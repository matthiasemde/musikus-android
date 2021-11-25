package de.practicetime.practicetime.entities;

import androidx.room.Entity
import androidx.room.Index

@Entity(primaryKeys = ["goalId", "categoryId"])
data class GoalCategoryCrossRef (
    val goalId: Int,
    val categoryId: Int,
)
