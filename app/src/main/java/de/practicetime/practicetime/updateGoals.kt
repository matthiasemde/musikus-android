package de.practicetime.practicetime
//
//import android.util.Log
//import androidx.lifecycle.LifecycleCoroutineScope
//import de.practicetime.practicetime.entities.Goal
//import de.practicetime.practicetime.entities.GoalPeriodUnit
//import de.practicetime.practicetime.entities.GoalType
//import de.practicetime.practicetime.entities.GoalWithCategories
//import kotlinx.coroutines.launch
//import java.util.*
//
//fun updateGoals(dao: PTDao, lifecycleScope: LifecycleCoroutineScope) {
//    lifecycleScope.launch {
//        var notDone = true
//        while(notDone) {
//            dao.getOutdatedGoalsWithCategories().also {
//                // while there are still outdated goals, keep looping and adding new ones
//                notDone = it.isNotEmpty()
//                it.forEach { (goal, categories) ->
//                    // create a new calendar instance, set the time to the goals start timestamp,...
//                    val startCalendar = Calendar.getInstance()
//                    startCalendar.timeInMillis = goal.startTimestamp * 1000L
//
////                    Log.d("updateGoals","BeforeAdd")
//
//                    // ... add to the calendar the period in period units...
//                    when (goal.periodUnit) {
//                        GoalPeriodUnit.DAY ->
//                            startCalendar.add(Calendar.DAY_OF_YEAR, goal.periodInPeriodUnits)
//                        GoalPeriodUnit.WEEK ->
//                            startCalendar.add(Calendar.WEEK_OF_YEAR, goal.periodInPeriodUnits)
//                        GoalPeriodUnit.MONTH ->
//                            startCalendar.add(Calendar.MONTH, goal.periodInPeriodUnits)
//                    }
//
//                    // ... and create a new goal with the same groupId, period and target
//                    dao.insertGoalWithCategories(
//                        GoalWithCategories(
//                            goal = computeNewGoal(
//                                groupId = goal.groupId,
//                                type = goal.type,
//                                timeFrame = startCalendar,
//                                periodUnit = goal.periodUnit,
//                                periodInPeriodUnits = goal.periodInPeriodUnits,
//                                target = goal.target
//                            ),
//                            categories
//                        )
//                    )
//
//                    // finally archive the original goal
//                    dao.archiveGoal(goal.id)
//                }
//            }
//        }
//    }
//}
