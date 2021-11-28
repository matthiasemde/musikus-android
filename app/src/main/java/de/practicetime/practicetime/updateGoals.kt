package de.practicetime.practicetime

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import de.practicetime.practicetime.entities.Goal
import de.practicetime.practicetime.entities.GoalPeriodUnit
import de.practicetime.practicetime.entities.GoalType
import de.practicetime.practicetime.entities.GoalWithCategories
import kotlinx.coroutines.launch
import java.util.*

fun updateGoals(dao: PTDao, lifecycleScope: LifecycleCoroutineScope) {
    lifecycleScope.launch {
        var notDone = true
        while(notDone) {
            dao.getOutdatedGoalsWithCategories().also {
                // while there are still outdated goals, keep looping and adding new ones
                notDone = it.isNotEmpty()
                it.forEach { (goal, categories) ->
                    // create a new calendar instance, set the time to the goals start timestamp,...
                    val startCalendar = Calendar.getInstance()
                    startCalendar.timeInMillis = goal.startTimestamp * 1000L

//                    Log.d("updateGoals","BeforeAdd")

                    // ... add to the calendar the period in period units...
                    when (goal.periodUnit) {
                        GoalPeriodUnit.DAY ->
                            startCalendar.add(Calendar.DAY_OF_YEAR, goal.periodInPeriodUnits)
                        GoalPeriodUnit.WEEK ->
                            startCalendar.add(Calendar.WEEK_OF_YEAR, goal.periodInPeriodUnits)
                        GoalPeriodUnit.MONTH ->
                            startCalendar.add(Calendar.MONTH, goal.periodInPeriodUnits)
                    }

                    // ... and create a new goal with the same groupId, period and target
                    dao.insertGoalWithCategories(
                        GoalWithCategories(
                            goal = computeNewGoal(
                                groupId = goal.groupId,
                                type = goal.type,
                                timeFrame = startCalendar,
                                periodUnit = goal.periodUnit,
                                periodInPeriodUnits = goal.periodInPeriodUnits,
                                target = goal.target
                            ),
                            categories
                        )
                    )

                    // finally archive the original goal
                    dao.archiveGoal(goal.id)
                }
            }
        }
    }
}

fun computeNewGoal(
    groupId: Int,
    type: GoalType,
    timeFrame: Calendar,
    periodInPeriodUnits: Int,
    periodUnit: GoalPeriodUnit,
    target: Int,
): Goal {
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

    return Goal(
        groupId = groupId,
        type = type,
        startTimestamp = startTimestamp,
        periodInSeconds = periodInSeconds,
        periodInPeriodUnits = periodInPeriodUnits,
        periodUnit = periodUnit,
        target = target
    )
}