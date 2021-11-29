package de.practicetime.practicetime.entities;

import androidx.room.Entity
import androidx.room.Index

@Entity(primaryKeys = ["goalDescriptionId", "categoryId"])
data class GoalDescriptionCategoryCrossRef (
    val goalDescriptionId: Int,
    val categoryId: Int,
)
