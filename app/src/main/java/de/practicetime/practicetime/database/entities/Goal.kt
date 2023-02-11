/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.database.entities

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import de.practicetime.practicetime.database.ModelWithTimestamps
import java.util.*

// shows, whether a goal will count all sections
// or only the one from specific libraryItems
enum class GoalType {
    NON_SPECIFIC, ITEM_SPECIFIC;

    companion object {
        fun toString(type: GoalType): String {
            return when (type) {
                NON_SPECIFIC -> "All items"
                ITEM_SPECIFIC -> "Specific item"
            }
        }
    }
}

// shows, whether a goal will track practice time
// or number of sessions
enum class GoalProgressType {
    TIME, SESSION_COUNT
}

enum class GoalPeriodUnit {
    DAY, WEEK, MONTH;

    companion object {
        fun toString(periodUnit: GoalPeriodUnit): String {
            return when (periodUnit) {
                DAY -> "Day"
                WEEK -> "Week"
                MONTH -> "Month"
            }
        }
    }
}

@Entity(
    tableName = "goal_instance",
    foreignKeys = [
        ForeignKey(
            entity = GoalDescription::class,
            parentColumns = ["id"],
            childColumns = ["goal_description_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GoalInstance(
    @ColumnInfo(name="goal_description_id", index = true) val goalDescriptionId: UUID,
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
//    @ColumnInfo(name="profile_id", index = true) val profileId: UUID? = null,
    @ColumnInfo(name="order", defaultValue = "0") var order: Int? = null,
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
