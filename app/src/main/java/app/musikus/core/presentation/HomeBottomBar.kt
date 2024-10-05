/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import app.musikus.core.presentation.components.MultiFabState

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun MusikusBottomBar(
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    currentTab: Screen.HomeTab?,
    onTabSelected: (Screen.HomeTab) -> Unit,
) {
    AnimatedVisibility(
        visible = currentTab != null, // only show bottom bar when in home screen
        enter = slideInVertically(
            initialOffsetY = { bottomBarHeight -> bottomBarHeight },
            animationSpec = tween(durationMillis = ANIMATION_BASE_DURATION)
        ),
        exit = slideOutVertically(
            targetOffsetY = { bottomBarHeight -> bottomBarHeight },
            animationSpec = tween(durationMillis = ANIMATION_BASE_DURATION)
        )
    ) {
        Box {
            NavigationBar {
                Screen.HomeTab.allTabs.forEach { tab ->
                    val selected = tab == currentTab
                    val painterCount = 5
                    var activePainter by remember { mutableIntStateOf(0) }
                    val painter = rememberVectorPainter(
                        image = tab.getDisplayData()?.icon?.asIcon()!!
                    )
                    val animatedPainters = (0..painterCount).map {
                        rememberAnimatedVectorPainter(
                            animatedImageVector = AnimatedImageVector.animatedVectorResource(
                                tab.getDisplayData()?.animatedIcon!!
                            ),
                            atEnd = selected && activePainter == it
                        )
                    }
                    NavigationBarItem(
                        icon = {
                            BadgedBox(badge = {
                                if (tab == Screen.HomeTab.Sessions && mainUiState.isSessionRunning) {
                                    Badge()
                                }
                            }) {
                                Image(
                                    painter = if (selected) animatedPainters[activePainter] else painter,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                    contentDescription = null
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.getDisplayData()?.title?.asAnnotatedString()!!,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                activePainter = (activePainter + 1) % painterCount
                                onTabSelected(tab)
                            }
                        }
                    )
                }
            }

            /** Navbar Scrim */
            AnimatedVisibility(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f),
                visible = mainUiState.multiFabState == MultiFabState.EXPANDED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { mainEventHandler(MainUiEvent.CollapseMultiFab) }
                        )
                )
            }
        }
    }
}
