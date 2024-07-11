/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.sessionslist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.utils.DurationFormat
import app.musikus.utils.getDurationString
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.utils.DateFormat
import app.musikus.utils.musikusFormat
import kotlin.time.Duration.Companion.seconds

@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    rating: Int = 5,
    total: Int = 7,
    image: ImageVector,
    color: Color = Color(0xFFFFB300),
    size: Dp = 16.dp,
    onRatingChanged: (Int) -> Unit = {}
) {
    Row(modifier) {
        for(i in 1..total) {
            Icon(
                modifier = Modifier
                    .size(size)
                    .clickable(
                        remember { MutableInteractionSource() },
                        indication = null
                    ) { onRatingChanged(i) },
                imageVector = image,
                tint = if (i <= rating) color else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = null
            )
        }
    }
}


@Composable
fun SessionCard(
    sessionWithSectionsWithLibraryItems: SessionWithSectionsWithLibraryItems,
) {
    val (session, sectionsWithLibraryItems) = sessionWithSectionsWithLibraryItems

    // compute the total practice time
    var practiceDuration = 0.seconds
    sectionsWithLibraryItems.forEach { (section, _) ->
        practiceDuration += section.duration
    }

    ElevatedCard {
        /** Card Header */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = sessionWithSectionsWithLibraryItems.startTimestamp.musikusFormat(
                    DateFormat.TIME_OF_DAY
                )
            )
            RatingBar(
                rating = session.rating,
                total = 5,
                image = Icons.Default.Star
            )
        }
        HorizontalDivider()

        /** Main Card content */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    modifier = Modifier
                        .width(0.dp)
                        .weight(4f),
                    text = getDurationString(practiceDuration, DurationFormat.HUMAN_PRETTY),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    modifier = Modifier
                        .width(0.dp)
                        .weight(2f),
                    text = getDurationString(session.breakDuration, DurationFormat.HUMAN_PRETTY),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Row {
                Text(
                    modifier = Modifier
                        .width(0.dp)
                        .weight(4f),
                    text = stringResource(id = R.string.sessionSummaryPracticeTime),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    modifier = Modifier
                        .width(0.dp)
                        .weight(2f),
                    text = stringResource(id = R.string.sessionSummaryBreakTime),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            sectionsWithLibraryItems.forEach { sectionWithLibraryItem ->
                Row {
                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .width(0.dp)
                            .weight(4f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(end = 8.dp)
                                .width(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(libraryItemColors[sectionWithLibraryItem.libraryItem.colorIndex])
                        )
                        Text(
                            text = sectionWithLibraryItem.libraryItem.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        modifier = Modifier
                            .width(0.dp)
                            .weight(2f),
                        text = getDurationString(
                            sectionWithLibraryItem.section.duration,
                            DurationFormat.HUMAN_PRETTY
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        /** Optional Comment Field */
        session.comment?.let { comment ->
            if (comment.isBlank()) return@let
            HorizontalDivider()
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.sessionSummaryComment),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        }
    }
}