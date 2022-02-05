package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["goalDescriptionId", "categoryId"])
data class GoalDescriptionCategoryCrossRef (
    @ColumnInfo(index = true) val goalDescriptionId: Long,
    @ColumnInfo(index = true) val categoryId: Long,
)
