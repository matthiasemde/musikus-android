/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2025 Michael Prommersberger
 */
package app.musikus.activesession.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musikus.R
import app.musikus.core.presentation.MainUiEvent
import app.musikus.core.presentation.components.SwipeToDeleteContainer
import app.musikus.core.presentation.components.fadingEdge
import app.musikus.core.presentation.theme.spacing


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SectionsList(
    uiState: State<ActiveSessionCompletedSectionsUiState?>,
    onSectionDeleted: (CompletedSectionUiState) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    listState: LazyListState,
    showSnackbar: (MainUiEvent.ShowSnackbar) -> Unit,
    additionalBottomContentPadding: Dp = 0.dp,
) {
    val listUiState = uiState.value ?: return

    // This column must not have padding to make swipe-to-dismiss work edge2edge
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large),
            textAlign = TextAlign.Start,
            text = stringResource(id = R.string.active_session_section_list_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fadingEdge(listState)
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .padding(
                    horizontal = MaterialTheme.spacing.large // main padding
                ),
            contentPadding = PaddingValues(bottom = additionalBottomContentPadding)
        ) {
            items(
                items = listUiState.items,
                key = { item -> item.id },
            ) { item ->
                SectionListElement(
                    modifier = Modifier.animateItem(),
                    item = item,
                    showSnackbar = showSnackbar,
                    onSectionDeleted = onSectionDeleted,
                )
            }
        }
    }

    // scroll to top when new item is added
    var sectionLen by remember { mutableIntStateOf(listUiState.items.size) }
    LaunchedEffect(key1 = listUiState.items) {
        if (listUiState.items.size > sectionLen && listState.canScrollBackward) {
            listState.animateScrollToItem(0)
        }
        sectionLen = listUiState.items.size
    }
}

@Composable
private fun SectionListElement(
    modifier: Modifier = Modifier,
    item: CompletedSectionUiState,
    showSnackbar: (MainUiEvent.ShowSnackbar) -> Unit,
    onSectionDeleted: (CompletedSectionUiState) -> Unit = {},
) {
    val context = LocalContext.current
    var deleted by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            deleted = targetValue == SwipeToDismissBoxValue.EndToStart
            true // don't set to deleted or item will not be dismissible again after restore
        },
        positionalThreshold = with(LocalDensity.current) {
            {
                100.dp.toPx()
            } // TODO remove hardcode?
        }
    )

    SwipeToDeleteContainer(
        state = dismissState,
        deleted = deleted,
        onDeleted = {
            onSectionDeleted(item)
            // as long as we don't have undo, we don't need to show a snackbar
//            showSnackbar(
//                MainUiEvent.ShowSnackbar(
//                    message = context.getString(R.string.active_session_sections_list_element_deleted),
//                    onUndo = { }
//                )
//            )
        }
    ) {
        Surface(
            // Surface for setting shape of item container
            modifier = modifier.height(50.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(item.color.copy(alpha = 0.6f)),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = item.durationText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            }
        }
    }
}
