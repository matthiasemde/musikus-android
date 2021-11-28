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
    val groupId: Int,
    val type: GoalType,
    val startTimestamp: Long,
    val periodInSeconds: Int,
    val periodInPeriodUnits: Int,
    val periodUnit: GoalPeriodUnit,
    var target: Int,
    val progressType: GoalProgressType = GoalProgressType.TIME,
    var progress: Int = 0,
    var archived: Boolean = false,
    var user_archived: Boolean = false,
    val profileId: Int = 0,
)