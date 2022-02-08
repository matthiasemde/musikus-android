package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import de.practicetime.practicetime.database.ModelWithTimestamps

@Entity(tableName = "category")
data class Category (
    @ColumnInfo(name="name") var name: String,
    @ColumnInfo(name="color_index") var colorIndex: Int,
    @ColumnInfo(name="archived") var archived: Boolean = false,
    @ColumnInfo(name="profile_id", index = true) val profileId: Int = 0,
    @ColumnInfo(name="order", defaultValue = "0") var order: Int = 0,
) : ModelWithTimestamps()