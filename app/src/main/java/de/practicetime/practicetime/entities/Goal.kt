package de.practicetime.practicetime.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalType {
    TIME, SESSION_COUNT
}

@Entity
data class Goal (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: GoalType = GoalType.TIME,
    val startTimestamp: Long,
    val period: Int,
    var target: Int,
    var progress: Int = 0,
    var archived: Boolean = false,
    val profileId: Int = 0,
)