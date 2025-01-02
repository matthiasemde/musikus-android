/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2024 Matthias Emde
 */

package app.musikus.goals.presentation

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.musikus.R
import app.musikus.core.data.GoalDescriptionWithLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.TimeProvider
import app.musikus.core.domain.TimeProviderImpl
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusPreviewElement1
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.asAnnotatedString
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.goals.data.daos.GoalDescription
import app.musikus.goals.data.daos.GoalInstance
import app.musikus.goals.data.entities.GoalPeriodUnit
import app.musikus.goals.data.entities.GoalProgressType
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.domain.GoalInstanceWithProgressAndDescriptionWithLibraryItems
import app.musikus.menu.domain.ColorSchemeSelections
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
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

    val libraryItemColor = if (description.type == GoalType.ITEM_SPECIFIC) {
        libraryItemColors[libraryItems.first().colorIndex]
    } else {
        null
    }

    ElevatedCard(
        modifier = modifier
            .blur(if (description.paused) 1.5.dp else 0.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        if (description.repeat) {
                            Icons.Rounded.Repeat
                        } else {
                            Icons.Filled.LocalFireDepartment
                        },
                        contentDescription = stringResource(
                            id = if (description.repeat) {
                                R.string.goals_repeating
                            } else {
                                R.string.goals_non_repeating
                            }
                        ),
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
                            text = if (remainingTime.isPositive()) stringResource(
                                R.string.core_time_left,
                                getDurationString(remainingTime, DurationFormat.PRETTY_APPROX)
                            ) else stringResource(R.string.goals_goal_card_expired),
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
                    label = "animatedGoalProgress"
                )

                val animatedProgressLeft = targetSeconds - animatedProgress
                val animatedProgressPercent = (animatedProgress / targetSeconds).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = libraryItemColor ?: MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        drawStopIndicator = {},
                        gapSize = 0.dp
                    )
                    Row(
                        modifier = Modifier
                            .matchParentSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (animatedProgressPercent < 1f) {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = getDurationString(
                                    animatedProgress.toInt().seconds,
                                    DurationFormat.HM_DIGITAL_OR_MIN_HUMAN
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = getDurationString(
                                    animatedProgressLeft.toInt().seconds,
                                    DurationFormat.HM_DIGITAL_OR_MIN_HUMAN
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
                                text = stringResource(id = R.string.goals_goal_card_achieved),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
            if (description.paused) {
                Icon(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center),
                    imageVector = Icons.Default.Pause,
                    contentDescription = "",
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

@MusikusPreviewElement1
@Composable
private fun PreviewGoalCard(
    @PreviewParameter(MusikusColorSchemeProvider::class) theme: ColorSchemeSelections,
) {
    val timeprovider = TimeProviderImpl()
    val startTime = timeprovider.now()

    val goal = GoalInstanceWithProgressAndDescriptionWithLibraryItems(
        description = GoalDescriptionWithLibraryItems(
            description = GoalDescription(
                id = UUIDConverter.fromInt(2),
                createdAt = startTime,
                modifiedAt = startTime,
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
                paused = false,
                archived = false,
                customOrder = null
            ),
            libraryItems = emptyList()
        ),
        instance = GoalInstance(
            id = UUIDConverter.fromInt(3),
            createdAt = startTime,
            modifiedAt = startTime,
            descriptionId = UUIDConverter.fromInt(2),
            previousInstanceId = null,
            startTimestamp = startTime,
            targetSeconds = 3600,
            endTimestamp = null
        ),
        progress = 1.hours
    )

    MusikusThemedPreview(theme = theme) {
        GoalCard(
            goal = goal,
            timeProvider = TimeProviderImpl()
        )
    }
}


@Composable
fun GoalProgressCard(
    icon: ImageVector,
    goalName: String,
    isAllLibrary: Boolean,
    timeLeft: String,
    duration: String,
    progress: Float,
    timeWorked: String,
    timeRemaining: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Icon and Goal Description
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAllLibrary) "All Library Items" else goalName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Goal Duration",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$duration • $timeLeft",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Metrics (Remaining and Worked Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Time Remaining",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeRemaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Time Worked",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeWorked,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewGoalProgressCard() {
    MaterialTheme {
        GoalProgressCard(
            icon = Icons.Default.Refresh,
            goalName = "Practice Andantino",
            isAllLibrary = false,
            timeLeft = "2 days left",
            duration = "1 week",
            progress = 0.75f,
            timeWorked = "1h 45m",
            timeRemaining = "2h 30m"
        )
    }
}




//##################################################################################
//##################################################################################
//##################################################################################
//##################################################################################
//##################################################################################
//##################################################################################


@Composable
fun GoalProgressCard2(
    icon: ImageVector,
    goalName: String,
    isAllLibrary: Boolean,
    totalDuration: String,
    remainingDays: String,
    durationProgress: Float,
    progress: Float,
    timeWorked: String,
    timeRemaining: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Icon and Goal Description
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAllLibrary) "All Library Items" else goalName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Time Remaining",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "1 Week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Time Remaining",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeRemaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowRight,
                        contentDescription = "Time Worked",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeWorked,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Column {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Metrics (Worked and Remaining)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "2 days left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

        }
    }
}

@Preview
@Composable
fun PreviewGoalProgressCard2() {
    MaterialTheme {
        GoalProgressCard2(
            icon = Icons.Default.Refresh,
            goalName = "Practice Andantino",
            isAllLibrary = false,
            totalDuration = "1 Week",
            remainingDays = "2 days",
            durationProgress = 0.71f,
            progress = 0.75f,
            timeWorked = "1h 45m",
            timeRemaining = "2h 30m"
        )
    }
}


// ############################################################################################################
// ############################################################################################################
// ############################################################################################################
// ############################################################################################################
// ############################################################################################################
// ############################################################################################################
// ############################################################################################################
// ############################################################################################################
// ############################################################################################################


@Composable
fun GoalProgressCard3(
    icon: ImageVector,
    goalName: String,
    isAllLibrary: Boolean,
    totalPeriod: String,
    daysLeft: String,
    progress: Float,
    timeWorked: String,
    timeRemaining: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 1. Item Name / All Items
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAllLibrary) "All Library Items" else goalName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Progress Bar
            Column {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 6. Percentage Completed
                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Days Left and 4. Total Period
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Days Left:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = daysLeft,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Period:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalPeriod,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Tracked Time & Remaining Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Time Remaining",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeRemaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Time Worked",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeWorked,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewGoalProgressCard3() {
    MaterialTheme {
        GoalProgressCard3(
            icon = Icons.Default.Refresh,
            goalName = "Practice Antantino",
            isAllLibrary = false,
            totalPeriod = "1 Week",
            daysLeft = "2 Days",
            progress = 0.75f,
            timeWorked = "1h 45m",
            timeRemaining = "2h 30m"
        )
    }
}