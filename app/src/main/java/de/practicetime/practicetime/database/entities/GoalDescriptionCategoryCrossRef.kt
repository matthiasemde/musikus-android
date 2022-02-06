package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "goal_description_category_cross_ref",
    primaryKeys = ["goal_description_id", "category_id"]
)
data class GoalDescriptionCategoryCrossRef (
    @ColumnInfo(name = "goal_description_id", index = true) val goalDescriptionId: Long,
    @ColumnInfo(name = "category_id", index = true) val categoryId: Long,
)
