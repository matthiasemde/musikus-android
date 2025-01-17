/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2025 Matthias Emde, Michael Prommersberger
 */

package app.musikus.goals.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign.Companion.End
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.components.TwoLiner
import app.musikus.core.presentation.components.TwoLinerData
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.UiText
import app.musikus.core.presentation.utils.asAnnotatedString
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.goals.data.entities.GoalType
import app.musikus.goals.domain.GoalInstanceWithProgressAndDescriptionWithLibraryItems
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

    val libraryItemColor = if (description.type == GoalType.ITEM_SPECIFIC) {
        libraryItemColors[libraryItems.first().colorIndex]
    } else {
        null
    }

    ElevatedCard(
        modifier = modifier
            .blur(if (description.paused) 1.5.dp else 0.dp),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    /** Goal Type */
                    Icon(
                        modifier = Modifier.size(24.dp),
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

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

                    TwoLiner(
                        modifier = Modifier.weight(1f),
                        data = TwoLinerData(
                            firstLine = goal.title,
                            secondLine = UiText.DynamicString(goal.subtitle.asAnnotatedString())
                        ),
                        paddingValues = PaddingValues(vertical = MaterialTheme.spacing.small)
                    )

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

                    /** remaining time */
                    val remainingTime = ChronoUnit.SECONDS.between(
                        timeProvider.now(),
                        goal.endTimestampInLocalTimezone(timeProvider)
                    ).seconds

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            modifier = Modifier.padding(MaterialTheme.spacing.small),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            text = if (remainingTime.isPositive()) {
                                stringResource(
                                    R.string.core_time_left,
                                    getDurationString(remainingTime, DurationFormat.PRETTY_APPROX)
                                )
                            } else {
                                stringResource(R.string.goals_goal_card_expired)
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

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

                // Time practiced
                Text(
                    text = getDurationString(
                        animatedProgress.toInt().seconds,
                        DurationFormat.HM_DIGITAL_OR_MIN_HUMAN
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                LinearProgressIndicator(
                    modifier = Modifier
                        .height(8.dp)
                        .fillMaxWidth(),
                    progress = { animatedProgressPercent },
                    color = libraryItemColor ?: MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                // Time left
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = getDurationString(
                        animatedProgressLeft.toInt().seconds,
                        DurationFormat.HM_DIGITAL_OR_MIN_HUMAN
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = End
                )
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
