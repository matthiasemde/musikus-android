/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.sessions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.core.presentation.MusikusTopBar
import app.musikus.core.presentation.theme.libraryItemColors
import app.musikus.core.presentation.utils.DurationFormat
import app.musikus.core.presentation.utils.UiText
import app.musikus.core.presentation.utils.getDurationString
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSession(
    editSessionViewModel: EditSessionViewModel = hiltViewModel(),
    sessionToEditId: UUID,
    navigateUp: () -> Unit
) {
    val editSessionUiState by editSessionViewModel.editSessionUiState.collectAsStateWithLifecycle()

    editSessionViewModel.setSessionToEditId(sessionToEditId)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MusikusTopBar(
                isTopLevel = false,
                title = UiText.DynamicString("Edit Session"),
                scrollBehavior = scrollBehavior,
                navigateUp = navigateUp
            )
        },
        content = { innerPadding ->
            val contentUiState = editSessionUiState.contentUiState
            val sessionEditData = contentUiState.sessionEditData
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "Your rating"
                )
                RatingBar(
                    image = Icons.Default.Star,
                    rating = sessionEditData.rating,
                    total = 5,
                    size = 24.dp,
                    onRatingChanged = editSessionViewModel::onRatingChanged,
                )
                Text(text = "Your session time")
                sessionEditData.sections.forEach { sectionWithLibraryItem ->
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
                Text(text = "Your comment")
                OutlinedTextField(
                    value = sessionEditData.comment,
                    onValueChange = editSessionViewModel::onCommentChanged,
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                content = {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = editSessionViewModel::onCancelHandler,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Cancel")
                    }
                    TextButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = editSessionViewModel::onConfirmHandler,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Edit")
                    }
                }
            )
        },
    )
}
