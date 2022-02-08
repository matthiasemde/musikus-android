package de.practicetime.practicetime.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import de.practicetime.practicetime.database.BaseModel

@Entity(tableName = "section")
data class Section (
    @ColumnInfo(name="session_id", index = true) var sessionId: Long?,
    @ColumnInfo(name="category_id", index = true) val categoryId: Long,
    @ColumnInfo(name="duration") var duration: Int?,
    @ColumnInfo(name="timestamp") val timestamp: Long,
) : BaseModel()