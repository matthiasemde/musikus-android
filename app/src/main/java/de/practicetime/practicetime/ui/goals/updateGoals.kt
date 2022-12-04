/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.ui.goals

import android.content.Context
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import java.util.*

suspend fun updateGoals(context: Context) {
    var notDone = true
    while(notDone) {
        PTDatabase.getInstance(context).goalInstanceDao.getOutdatedWithDescriptions().also { outdatedInstancesWithDescriptions ->
            // while there are still outdated goals, keep looping and adding new ones
            notDone = outdatedInstancesWithDescriptions.isNotEmpty()
            outdatedInstancesWithDescriptions.forEach { (outdatedInstance, description) ->
                if (description.repeat && !description.archived) {

                    // create a new calendar instance, set the time to the instances start timestamp,...
                    val startCalendar = Calendar.getInstance()
                    startCalendar.timeInMillis = outdatedInstance.startTimestamp * 1000L

                    // ... add to the calendar the period in period units...
                    when (description.periodUnit) {
                        GoalPeriodUnit.DAY ->
                            startCalendar.add(
                                Calendar.DAY_OF_YEAR,
                                description.periodInPeriodUnits
                            )
                        GoalPeriodUnit.WEEK ->
                            startCalendar.add(
                                Calendar.WEEK_OF_YEAR,
                                description.periodInPeriodUnits
                            )
                        GoalPeriodUnit.MONTH ->
                            startCalendar.add(Calendar.MONTH, description.periodInPeriodUnits)
                    }

                    // ... and create a new goal with the same groupId, period and target
                    PTDatabase.getInstance(context).goalInstanceDao.insert(
                        description.createInstance(
                            timeFrame = startCalendar,
                            target = outdatedInstance.target
                        )
                    )
                } else if(!description.archived) {
                    PTDatabase.getInstance(context).goalDescriptionDao.archive(description)
                }

                // finally mark the outdated instance as renewed
                outdatedInstance.renewed = true
                PTDatabase.getInstance(context).goalInstanceDao.update(outdatedInstance)
            }
        }
    }
}
