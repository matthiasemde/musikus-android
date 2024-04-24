package app.musikus.ui.components

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Michael Prommersberger
 *
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.musikus.ui.theme.spacing
import kotlinx.coroutines.delay


/**
 * A container that allows the content to be swiped away.
 *
 * Wrap your content with this container to enable swipe to delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDeleted: () -> Unit,
    state: SwipeToDismissBoxState,
    deleted: Boolean,
    animationDuration: Int = 500,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = !deleted,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = animationDuration),
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = state,
            enableDismissFromEndToStart = true,     // <-<-<-
            enableDismissFromStartToEnd = false,    // ->->-> (deactivate)
            backgroundContent = {
                SwipeToDeleteBackground(dismissState = state)
            }
        ) {
            content()
        }
    }

    // actually delete the element from the list (business logic)
    LaunchedEffect(key1 = deleted){
        if (state.targetValue == SwipeToDismissBoxValue.EndToStart) {
            delay(animationDuration.toLong())
            onDeleted()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteBackground(
    dismissState: SwipeToDismissBoxState
) {
    val color by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        label = "swipeDismissAnimation",
    )
    val iconColor by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.error
        },
        label = "swipeDismissAnimationIcon"
    )
    Box(
        Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(color)
            .padding(end = MaterialTheme.spacing.medium),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete section",
            tint = iconColor,
        )
    }
}