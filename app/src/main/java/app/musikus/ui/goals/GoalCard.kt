/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.ui.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.musikus.R
import app.musikus.database.entities.GoalType
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
import app.musikus.ui.theme.libraryItemColors
import app.musikus.usecase.goals.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.utils.TimeProvider
import app.musikus.utils.asAnnotatedString
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun GoalCard(
    modifier: Modifier = Modifier,
    goal: GoalInstanceWithProgressAndDescriptionWithLibraryItems,
    progressOffset: Duration = 0.seconds,
    timeProvider: TimeProvider
) {

    val descriptionWithLibraryItems = goal.description
    val (description, libraryItems) = descriptionWithLibraryItems
    val progress = goal.progress

    val libraryItemColor = if(description.type == GoalType.ITEM_SPECIFIC) {
        libraryItemColors[libraryItems.first().colorIndex]
    } else null

    ElevatedCard(modifier = modifier
        .blur(if (description.paused) 1.5.dp else 0.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
//                    Row(
//                        modifier = Modifier
//                            .height(IntrinsicSize.Min)
//                    ) {
                        /** Goal Type */
                        Icon(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(24.dp),
                            imageVector =
                                if(description.repeat) Icons.Rounded.Repeat
                                else Icons.Filled.LocalFireDepartment,
                            contentDescription = if(description.repeat)
                                "Regular goal" else "One shot goal",
                            tint = libraryItemColor ?: MaterialTheme.colorScheme.primary
                        )


                        /** Color indicator */
//                        libraryItemColor?.let {
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxHeight()
//                                    .padding(end = 8.dp)
//                                    .width(6.dp)
//                                    .clip(RoundedCornerShape(4.dp))
//                                    .background(it)
//                            )
//                        }

                        /** Goal Title */
                        Text(
                            modifier = Modifier.weight(1f),
                            text = goal.title.asAnnotatedString()
                        )
//                    }

                    /** remaining time */
                    val remainingTime = ChronoUnit.SECONDS.between(
                        timeProvider.now(),
                        goal.endTimestampInLocalTimezone(timeProvider)
                    ).seconds

                    Surface(
                        modifier = Modifier.padding(start = 8.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 5.dp
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            maxLines = 1,
                            text= stringResource(
                                R.string.time_left,
                                getDurationString(remainingTime, DurationFormat.PRETTY_APPROX)
                            )
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                /** Goal Description */
                Text(
                    text = goal.subtitle.asAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
                )

                /** ProgressBar */
                val targetSeconds = goal.instance.target.inWholeSeconds
                val animatedProgress by animateFloatAsState(
                    targetValue = (progress + progressOffset)
                        .inWholeSeconds
                        .coerceAtMost(targetSeconds)
                        .toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh / goal.instance.targetSeconds
                    ),
                    label = "animated goal progress"
                )

                val animatedProgressLeft = targetSeconds - animatedProgress
                val animatedProgressPercent = (animatedProgress / targetSeconds).coerceIn(0f, 1f)

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
                        libraryItemColor ?: MaterialTheme.colorScheme.primary,
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
                                    animatedProgress.toInt().seconds,
                                    DurationFormat.HUMAN_PRETTY
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = getDurationString(
                                    animatedProgressLeft.toInt().seconds,
                                    DurationFormat.HUMAN_PRETTY
                                ),
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
            if(description.paused) {
                Icon(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Paused Goal",
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))

                )
            }
        }
    }
}