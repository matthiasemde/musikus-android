package de.practicetime.practicetime.database.entities

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

// shows, whether a goal will count all sections or only the one from specific categories
enum class GoalType {
    NON_SPECIFIC, CATEGORY_SPECIFIC
}

enum class GoalProgressType {
    TIME, SESSION_COUNT
}

enum class GoalPeriodUnit {
    DAY, WEEK, MONTH
}

@Entity
class GoalDescription (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: GoalType,
    val oneTime: Boolean,
    val periodInPeriodUnits: Int,
    val periodUnit: GoalPeriodUnit,
    val progressType: GoalProgressType = GoalProgressType.TIME,
    var archived: Boolean = false,
    val profileId: Int = 0,
) {

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

@Entity
data class GoalInstance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalDescriptionId: Int,
    val startTimestamp: Long,
    val periodInSeconds: Int,
    var target: Int,
    var progress: Int = 0,
    var renewed: Boolean = false,
)