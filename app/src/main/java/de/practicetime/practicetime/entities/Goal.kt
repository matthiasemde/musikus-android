package de.practicetime.practicetime.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


enum class GoalType {
    TOTAL_TIME, SPECIFIC_CATEGORIES
}

enum class GoalProgressType {
    TIME, SESSION_COUNT
}

enum class GoalPeriodUnit {
    DAY, WEEK, MONTH
}

@Entity
data class Goal (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: GoalType,
    val progressType: GoalProgressType = GoalProgressType.TIME,
    val startTimestamp: Long,
    val period: Int,
    val periodUnit: GoalPeriodUnit,
    var target: Int,
    var progress: Int = 0,
    var archived: Boolean = false,
    var user_archived: Boolean = false,
    val profileId: Int = 0,
)