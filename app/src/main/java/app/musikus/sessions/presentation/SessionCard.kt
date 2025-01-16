/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022-2025 Matthias Emde
 */

package app.musikus.sessions.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.data.SectionWithLibraryItem
import app.musikus.core.data.SessionWithSectionsWithLibraryItems
import app.musikus.core.data.UUIDConverter
import app.musikus.core.domain.DateFormat
import app.musikus.core.domain.musikusFormat
import app.musikus.core.presentation.theme.MusikusColorSchemeProvider
import app.musikus.core.presentation.theme.MusikusThemedPreview
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.theme.spacing
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.getDurationString
import app.musikus.library.data.daos.LibraryItem
import app.musikus.menu.domain.ColorSchemeSelections
import app.musikus.sessions.data.daos.Section
import app.musikus.sessions.data.daos.Session
import java.time.ZonedDateTime
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
    val ratingBarContentDescription = stringResource(
        id = R.string.components_rating_bar_rating_description,
        rating,
        total
    )

    Row(
        modifier = modifier.semantics {
            contentDescription = ratingBarContentDescription
        }
    ) {
        for (i in 1..total) {
            Icon(
                modifier = Modifier
                    .size(size)
                    .clickable(
                        remember { MutableInteractionSource() },
                        indication = null
                    ) { onRatingChanged(i) },
                imageVector = image,
                tint = if (i <= rating) {
                    color
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                contentDescription = stringResource(
                    id = R.string.components_rating_bar_individual_rating_description,
                    i,
                    total
                )
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

    // used as ratio for the two columns
    val goldenRatio = 1.618f

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
                    modifier = Modifier.weight(goldenRatio),
                    text = getDurationString(practiceDuration, DurationFormat.HUMAN_PRETTY),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = getDurationString(session.breakDuration, DurationFormat.HUMAN_PRETTY),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Row {
                Text(
                    modifier = Modifier.weight(goldenRatio),
                    text = stringResource(id = R.string.sessions_session_card_practice_time),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = R.string.sessions_session_card_break_time),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            sectionsWithLibraryItems.forEach { sectionWithLibraryItem ->
                Row {
                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .weight(goldenRatio)
                    ) {
                        val item = sectionWithLibraryItem.libraryItem
                        Surface(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(),
                            shape = MaterialTheme.shapes.small,
                            color = libraryItemColors[item.colorIndex]
                        ) { }
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = getDurationString(
                            sectionWithLibraryItem.section.duration,
                            DurationFormat.HUMAN_PRETTY
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            }
        }
        /** Optional Comment Field */
        session.comment?.let { comment ->
            if (comment.isBlank()) return@let
            HorizontalDivider()
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                Text(
                    text = stringResource(id = R.string.sessions_session_card_comment),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
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

@PreviewLightDark
@Composable
private fun PreviewSessionCard(
    @PreviewParameter(MusikusColorSchemeProvider::class) colorScheme: ColorSchemeSelections
) {
    MusikusThemedPreview(colorScheme) {
        SessionCard(
            sessionWithSectionsWithLibraryItems = SessionWithSectionsWithLibraryItems(
                session = Session(
                    id = UUIDConverter.deadBeef,
                    createdAt = ZonedDateTime.now(),
                    modifiedAt = ZonedDateTime.now(),
                    breakDurationSeconds = 600,
                    rating = 5,
                    comment = "This is a comment"
                ),
                sections = listOf(
                    SectionWithLibraryItem(
                        section = Section(
                            id = UUIDConverter.deadBeef,
                            startTimestamp = ZonedDateTime.now(),
                            durationSeconds = 1800,
                            libraryItemId = UUIDConverter.deadBeef,
                            sessionId = UUIDConverter.deadBeef
                        ),
                        libraryItem = LibraryItem(
                            id = UUIDConverter.deadBeef,
                            name = "Item 1",
                            colorIndex = 0,
                            createdAt = ZonedDateTime.now(),
                            modifiedAt = ZonedDateTime.now(),
                            libraryFolderId = null,
                            customOrder = null
                        )
                    )
                )
            )
        )
    }
}
