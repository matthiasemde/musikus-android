/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.ui.goals

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.chip.Chip
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.utils.*

@SuppressLint("ViewConstructor")
class GoalCard(
    context: Context,
    goalInstanceWithDescriptionWithLibraryItems: GoalInstanceWithDescriptionWithLibraryItems
): LinearLayout(context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.listitem_goal, null)

    private val progressBarView: ProgressBar = view.findViewById(R.id.goalProgressBar)
    private val goalNameView: TextView = view.findViewById(R.id.goalName)
    private val goalDescriptionView: TextView = view.findViewById(R.id.goalDescription)
    private val remainingTimeView: Chip = view.findViewById(R.id.goalRemainingTime)
    private val goalProgressDoneIndicatorView: TextView = view.findViewById(R.id.goalProgressDoneIndicator)
    private val goalProgressLeftIndicatorView: TextView = view.findViewById(R.id.goalProgressLeftIndicator)
    private val goalProgressAchievedView: TextView = view.findViewById(R.id.goalProgressAchieved)
    private val sectionColorView: ImageView = view.findViewById(R.id.sectionColor)

    init {
        val (instance, descriptionWithLibraryItems) = goalInstanceWithDescriptionWithLibraryItems
        val (description, libraryItems) = descriptionWithLibraryItems

        // get the libraryItem color for later use in different UI elements
        var libraryItemColor: ColorStateList? = null
        if(description.type != GoalType.NON_SPECIFIC) {
            libraryItemColor = ColorStateList.valueOf(
                context.resources.getIntArray(R.array.library_item_colors)[libraryItems.firstOrNull()?.colorIndex ?: 0]
            )
        }

        /** Goal Title */
        if(description.type == GoalType.NON_SPECIFIC) {
            goalNameView.text = context.getString(R.string.goal_name_non_specific)
        } else {
            goalNameView.apply {
                text = libraryItems.firstOrNull()?.name ?: "Delete me!"
            }
        }

        /** Goal Description */
        val count = description.periodInPeriodUnits
        val periodFormatted =
            when (description.periodUnit) {
                GoalPeriodUnit.DAY -> context.resources.getQuantityString(R.plurals.time_period_day, count, count)
                GoalPeriodUnit.WEEK -> context.resources.getQuantityString(R.plurals.time_period_week, count, count)
                GoalPeriodUnit.MONTH -> context.resources.getQuantityString(R.plurals.time_period_month, count, count)
            }

        goalDescriptionView.text = TextUtils.concat(
            getDurationString(instance.target, TIME_FORMAT_HUMAN_PRETTY),
            " ",
            periodFormatted
        )

        /** ProgressBar */
        progressBarView.max = instance.target
        progressBarView.progress = instance.progress

        // tint progressbar
        if(description.type != GoalType.NON_SPECIFIC) {
            sectionColorView.visibility = View.VISIBLE
            progressBarView.progressTintList = libraryItemColor
            sectionColorView.backgroundTintList = libraryItemColor
        } else {
            sectionColorView.visibility = View.GONE
            progressBarView.progressTintList = null
        }

        /** progress Indicator Text */
        val progressLeft = maxOf(0, instance.target - instance.progress)
        if(progressLeft > 0) {

            goalProgressDoneIndicatorView.text = getDurationString(
                instance.progress,
                TIME_FORMAT_HUMAN_PRETTY,
                SCALE_FACTOR_FOR_SMALL_TEXT
            )

            goalProgressLeftIndicatorView.text = getDurationString(
                progressLeft,
                TIME_FORMAT_HUMAN_PRETTY,
                SCALE_FACTOR_FOR_SMALL_TEXT
            )

            goalProgressAchievedView.visibility = View.INVISIBLE
            goalProgressDoneIndicatorView.visibility = View.VISIBLE
            goalProgressLeftIndicatorView.visibility = View.VISIBLE
        } else {
            goalProgressAchievedView.visibility = View.VISIBLE
            goalProgressDoneIndicatorView.visibility = View.GONE
            goalProgressLeftIndicatorView.visibility = View.GONE
        }

        // remaining time
        val remainingTime = (instance.startTimestamp + instance.periodInSeconds) - getCurrTimestamp()

        remainingTimeView.text = context.getString(
            R.string.time_left,
            getDurationString(remainingTime.toInt(), TIME_FORMAT_PRETTY_APPROX)
        )

        super.addView(view)
    }

    fun update() {

    }
}