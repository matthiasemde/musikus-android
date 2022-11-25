/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.entities

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import de.practicetime.practicetime.database.ModelWithTimestamps
import java.util.*

// shows, whether a goal will count all sections
// or only the one from specific categories
enum class GoalType {
    NON_SPECIFIC, CATEGORY_SPECIFIC
}

// shows, whether a goal will track practice time
// or number of sessions
enum class GoalProgressType {
    TIME, SESSION_COUNT
}

enum class GoalPeriodUnit {
    DAY, WEEK, MONTH
}

@Entity(tableName = "goal_instance")
data class GoalInstance(
    @ColumnInfo(name="goal_description_id") val goalDescriptionId: Long,
    @ColumnInfo(name="start_timestamp") val startTimestamp: Long,
    @ColumnInfo(name="period_in_seconds") val periodInSeconds: Int,
    @ColumnInfo(name="target") var target: Int,
    @ColumnInfo(name="progress") var progress: Int = 0,
    @ColumnInfo(name="renewed") var renewed: Boolean = false,
) : ModelWithTimestamps()


@Entity(tableName = "goal_description")
class GoalDescription (
    @ColumnInfo(name="type") val type: GoalType,
    @ColumnInfo(name="repeat") val repeat: Boolean,
    @ColumnInfo(name="period_in_period_units") val periodInPeriodUnits: Int,
    @ColumnInfo(name="period_unit") val periodUnit: GoalPeriodUnit,
    @ColumnInfo(name="progress_type") val progressType: GoalProgressType = GoalProgressType.TIME,
    @ColumnInfo(name="archived") var archived: Boolean = false,
    @ColumnInfo(name="profile_id", index = true) val profileId: Int = 0,
    @ColumnInfo(name="order", defaultValue = "0") var order: Int = 0,
    ) : ModelWithTimestamps() {

    // create a new instance of this goal, storing the target and progress during a single period
    fun createInstance(
        timeFrame: Calendar,
        target: Int,
    ): GoalInstance {
        var startTimestamp = 0L

        // to find the correct starting point and period for the goal, we execute these steps:
        // 1. clear the minutes, seconds and millis from the time frame and set hour to 0
        // 2. set the time frame to the beginning of the day, week or month
        // 3. save the time in seconds as startTimeStamp
        // 4. then set the day to the end of the period according to the periodInPeriodUnits
        // 5. calculate the period in seconds from the difference of the two timestamps
        timeFrame.clear(Calendar.MINUTE)
        timeFrame.clear(Calendar.SECOND)
        timeFrame.clear(Calendar.MILLISECOND)
        timeFrame.set(Calendar.HOUR_OF_DAY, 0)

        when(periodUnit) {
            GoalPeriodUnit.DAY -> {
                startTimestamp = timeFrame.timeInMillis / 1000L
                timeFrame.add(Calendar.DAY_OF_YEAR, periodInPeriodUnits)
            }
            GoalPeriodUnit.WEEK -> {
                if(timeFrame.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    timeFrame.add(Calendar.DAY_OF_WEEK, - 1)
                }
                timeFrame.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                startTimestamp = timeFrame.timeInMillis / 1000L

                timeFrame.add(Calendar.WEEK_OF_YEAR, periodInPeriodUnits)
            }
            GoalPeriodUnit.MONTH -> {
                timeFrame.set(Calendar.DAY_OF_MONTH, 1)
                startTimestamp = timeFrame.timeInMillis / 1000L

                timeFrame.add(Calendar.MONTH, periodInPeriodUnits)
            }
        }

        // calculate the period in second from these two timestamps
        val periodInSeconds = ((timeFrame.timeInMillis / 1000) - startTimestamp).toInt()

        assert(startTimestamp > 0) {
            Log.e("Assertion Failed", "startTimeStamp can not be 0")
        }

        return GoalInstance(
            goalDescriptionId = id,
            startTimestamp = startTimestamp,
            periodInSeconds = periodInSeconds,
            target = target
        )
    }
}
