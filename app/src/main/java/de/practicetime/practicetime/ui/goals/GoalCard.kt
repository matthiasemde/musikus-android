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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import com.google.android.material.chip.Chip
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.utils.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("ViewConstructor")
class GoalCard(
    context: Context,
    goalInstanceWithDescriptionWithLibraryItems: GoalInstanceWithDescriptionWithLibraryItems
): LinearLayout(context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.listitem_goal, null)

    private val progressBarComposeView: ComposeView = view.findViewById(R.id.goalProgressBarC)
    private val goalNameView: TextView = view.findViewById(R.id.goalName)
    private val goalDescriptionView: TextView = view.findViewById(R.id.goalDescription)
    private val remainingTimeView: Chip = view.findViewById(R.id.goalRemainingTime)
    private val sectionColorView: ImageView = view.findViewById(R.id.sectionColor)

    init {
        val (instance, descriptionWithLibraryItems) = goalInstanceWithDescriptionWithLibraryItems
        val (description, libraryItems) = descriptionWithLibraryItems

        // get the libraryItem color for later use in different UI elements
        val libraryItemColor = if(description.type != GoalType.NON_SPECIFIC) {
            PracticeTime.getLibraryItemColors(context)[libraryItems.first().colorIndex]
        } else null

        // Library Item indicator
        if(libraryItemColor != null) {
            sectionColorView.visibility = View.VISIBLE
            sectionColorView.backgroundTintList = ColorStateList.valueOf(libraryItemColor)
        } else {
            sectionColorView.visibility = View.GONE
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

        /** ProgressBar Composed */
        progressBarComposeView.setContent {
            var progress by remember { mutableStateOf(
                goalInstanceWithDescriptionWithLibraryItems.instance.progress
            ) }
            val target = goalInstanceWithDescriptionWithLibraryItems.instance.target
            val animatedProgress by animateIntAsState(
                targetValue = progress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessVeryLow / 5 // TODO normalize???
                )
            )
            LaunchedEffect(true) {
                delay(1000)
                progress += 2000
            }
            val animatedProgressLeft = target - animatedProgress
            val animatedProgressPercent = (animatedProgress.toFloat() / target.toFloat()).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                    ,
                    progress = animatedProgressPercent,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    color =
                    if (libraryItemColor != null) {
                        Color(libraryItemColor)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Row(
                    modifier = Modifier
                        .matchParentSize()
                       ,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if(animatedProgressPercent < 1f) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = getDurationString(
                                animatedProgress,
                                TIME_FORMAT_HUMAN_PRETTY,
                                SCALE_FACTOR_FOR_SMALL_TEXT
                            ).toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = getDurationString(
                                animatedProgressLeft,
                                TIME_FORMAT_HUMAN_PRETTY,
                                SCALE_FACTOR_FOR_SMALL_TEXT
                            ).toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    AnimatedVisibility(
                        visible = animatedProgressPercent == 1f,
                        enter = scaleIn(
                            spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        ),
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = stringResource(id = R.string.goal_description_achieved),
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
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