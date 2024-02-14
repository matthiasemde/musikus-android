/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.ui.MainUiEventHandler
import app.musikus.ui.MainUiState
import app.musikus.ui.Screen
import app.musikus.ui.components.MultiFabState
import app.musikus.ui.goals.GoalsScreen
import app.musikus.ui.library.Library
import app.musikus.ui.sessions.SessionsScreen
import app.musikus.ui.statistics.Statistics
import app.musikus.utils.TimeProvider


@Composable
fun HomeScreen(
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    viewModel: HomeViewModel = hiltViewModel(),
    navigateTo: (Screen) -> Unit,
    timeProvider: TimeProvider,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler: HomeUiEventHandler = viewModel::onUiEvent

    BackHandler(
        enabled = uiState.currentTab != Screen.HomeTab.defaultTab,
        onBack = { eventHandler(HomeUiEvent.TabSelected(Screen.HomeTab.defaultTab)) }
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = mainUiState.snackbarHost)
        },
        bottomBar = {
            MusikusBottomBar(
                homeUiState = uiState,
                homeEventHandler = eventHandler,
                onTabSelected = {
                    eventHandler(HomeUiEvent.TabSelected(it))
                },
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            modifier = Modifier.padding(innerPadding),
            targetState = uiState.currentTab,
            label = "homeTabContent"
        ) { currentTab ->
            when(currentTab) {
                is Screen.HomeTab.Sessions -> {
                    SessionsScreen(
                        mainEventHandler = mainEventHandler,
                        homeUiState = uiState,
                        homeEventHandler = eventHandler,
                        navigateTo = navigateTo,
                        onSessionEdit = {}
//                        onSessionEdit = { sessionId: UUID ->
//                            navController.navigate(
//                                Screen.EditSession.route.replace(
//                                    "{sessionId}",
//                                    sessionId.toString()
//                                )
//                            )
//                        },
                    )
                }
                is Screen.HomeTab.Goals -> {
                    GoalsScreen(
                        mainEventHandler = mainEventHandler,
                        homeUiState = uiState,
                        homeEventHandler = eventHandler,
                        navigateTo = navigateTo,
                        timeProvider = timeProvider
                    )
                }
                is Screen.HomeTab.Library -> {
                    Library (
                        mainEventHandler = mainEventHandler,
                        homeUiState = uiState,
                        homeEventHandler = eventHandler,
                        navigateTo = navigateTo
                    )
                }
                is Screen.HomeTab.Statistics -> {
                    Statistics(
                        homeUiState = uiState,
                        homeEventHandler = eventHandler,
                        navigateTo = navigateTo,
                        timeProvider = timeProvider
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun MusikusBottomBar(
    homeUiState: HomeUiState,
    homeEventHandler: HomeUiEventHandler,
    onTabSelected: (Screen.HomeTab) -> Unit,
) {

    Box {
        NavigationBar {
            Screen.HomeTab.allTabs.forEach { tab ->
                val selected = tab == homeUiState.currentTab
                val painterCount = 5
                var activePainter by remember { mutableIntStateOf(0) }
                val painter = rememberVectorPainter(
                    image = tab.displayData!!.icon.asIcon()
                )
                val animatedPainters = (0..painterCount).map {
                    rememberAnimatedVectorPainter(
                        animatedImageVector = AnimatedImageVector.animatedVectorResource(
                            tab.displayData.animatedIcon!!
                        ),
                        atEnd = selected && activePainter == it
                    )
                }
                NavigationBarItem(
                    icon = {
                        Image(
                            painter = if (selected) animatedPainters[activePainter] else painter,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = tab.displayData.title.asAnnotatedString(),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            activePainter = (activePainter + 1) % painterCount
                        }
                        onTabSelected(tab)
                    }
                )
            }
        }

        /** Navbar Scrim */
        AnimatedVisibility(
            modifier = Modifier
                .matchParentSize()
                .zIndex(1f),
            visible = homeUiState.multiFabState == MultiFabState.EXPANDED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { homeEventHandler(HomeUiEvent.CollapseMultiFab) }
                    )
            )
        }
    }
}