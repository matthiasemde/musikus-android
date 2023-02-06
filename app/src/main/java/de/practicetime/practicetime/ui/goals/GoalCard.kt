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
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.utils.*

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("ViewConstructor")
class GoalCard(
    context: Context,
    private val goalInstanceWithDescriptionWithLibraryItems: GoalInstanceWithDescriptionWithLibraryItems
): LinearLayout(context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.listitem_goal, null)

//    private val progressBarComposeView: ComposeView = view.findViewById(R.id.goalProgressBar)
    private val goalNameView: TextView = view.findViewById(R.id.goalName)
    private val goalDescriptionView: TextView = view.findViewById(R.id.goalDescription)
//    private val remainingTimeView: Chip = view.findViewById(R.id.goalRemainingTime)
    private val sectionColorView: ImageView = view.findViewById(R.id.sectionColor)

    private var libraryItemColor: Int? = null

    init {
        val (instance, descriptionWithLibraryItems) = goalInstanceWithDescriptionWithLibraryItems
        val (description, libraryItems) = descriptionWithLibraryItems

        // get the libraryItem color for later use in different UI elements
        libraryItemColor = if(description.type != GoalType.NON_SPECIFIC) {
            PracticeTime.getLibraryItemColors(context)[libraryItems.first().colorIndex]
        } else null

        // Library Item indicator
        libraryItemColor?.let {
            sectionColorView.visibility = View.VISIBLE
            sectionColorView.backgroundTintList = ColorStateList.valueOf(it)
        } ?: run {
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



        // remaining time
        val remainingTime = (instance.startTimestamp + instance.periodInSeconds) - getCurrTimestamp()

//        remainingTimeView.text = context.getString(
//            R.string.time_left,
//            getDurationString(remainingTime.toInt(), TIME_FORMAT_PRETTY_APPROX)
//        )

        super.addView(view)
    }

    fun update(
        goal: GoalInstanceWithDescriptionWithLibraryItems
    ) {
        Log.d("GOAL_CARD", "inside update ${goal.instance.target}")

        /** ProgressBar Composed */
        if(getCurrTimestamp().toInt() % 2 == 0) {
            view.findViewById<ComposeView>(R.id.goalProgressBar).setContent {
                Log.d("GOAL_CARD", "setContentFirst")
                Text(
                    text = "Hello World",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
        else {
            view.findViewById<ComposeView>(R.id.goalProgressBar).setContent {
                Log.d("GOAL_CARD", "setContent")
                val progress by remember { mutableStateOf(
//                    goal.instance.progress
                    0
                ) }
                val target = goal.instance.target
                val animatedProgress by animateIntAsState(
                    targetValue = progress,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessVeryLow / 5 // TODO normalize???
                    )
                )
    //            LaunchedEffect(true) {
    //                delay(1000)
    //                progress += 2000
    //            }
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
                        libraryItemColor?.let {
                            Color(it)
                        } ?: MaterialTheme.colorScheme.inversePrimary,
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun GoalCard(
    modifier: Modifier = Modifier,
    goal: GoalInstanceWithDescriptionWithLibraryItems,
    progress: Int = 0,
    progressOffset: Int = 0
) {
    val (instance, descriptionWithLibraryItems) = goal
    val (description, libraryItems) = descriptionWithLibraryItems

    val libraryItemColor = if(description.type == GoalType.ITEM_SPECIFIC) {
        Color(PracticeTime.getLibraryItemColors(LocalContext.current)[libraryItems.first().colorIndex])
    } else null

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                ) {
                    /** Color indicator */
                    libraryItemColor?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(end = 8.dp)
                                .width(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(it)
                        )
                    }

                    /** Goal Title */
                    Text(text = if(description.type == GoalType.NON_SPECIFIC) {
                        stringResource(R.string.goal_name_non_specific)
                    } else {
                        libraryItems.firstOrNull()?.name ?: "Delete me!"
                    })
                }

                /** remaining time */
                val remainingTime = (instance.startTimestamp + instance.periodInSeconds) - getCurrTimestamp()

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 5.dp
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text= stringResource(
                            R.string.time_left,
                            getDurationString(remainingTime.toInt(), TIME_FORMAT_PRETTY_APPROX)
                        )
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            /** Goal Description */
            val count = description.periodInPeriodUnits
            val periodFormatted =
                when (description.periodUnit) {
                    GoalPeriodUnit.DAY -> pluralStringResource(R.plurals.time_period_day, count, count)
                    GoalPeriodUnit.WEEK -> pluralStringResource(R.plurals.time_period_week, count, count)
                    GoalPeriodUnit.MONTH -> pluralStringResource(R.plurals.time_period_month, count, count)
                }

            Text(
                text = TextUtils.concat(
                    getDurationString(instance.target, TIME_FORMAT_HUMAN_PRETTY),
                    " ",
                    periodFormatted).toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
            )

            /** ProgressBar */
            val target = goal.instance.target
            val animatedProgress by animateIntAsState(
                targetValue = progress + progressOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh / goal.instance.target
                )
            )

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
                    libraryItemColor ?: MaterialTheme.colorScheme.inversePrimary,
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
                                stiffness = Spring.StiffnessVeryLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        ),
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = stringResource(id = R.string.goal_description_achieved),
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}