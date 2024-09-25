/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.core.domain.TimeProvider
import app.musikus.goals.presentation.GoalsScreen
import app.musikus.library.presentation.Library
import app.musikus.sessions.presentation.SessionsScreen
import app.musikus.statistics.presentation.Statistics

@Composable
fun HomeScreen(
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    bottomBarHeight: Dp,
    currentTab: Screen.HomeTab,
    viewModel: HomeViewModel = hiltViewModel(),
    navigateTo: (Screen) -> Unit,
    timeProvider: TimeProvider,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler: HomeUiEventHandler = viewModel::onUiEvent

    when(currentTab) {
        is Screen.HomeTab.Sessions -> {
            SessionsScreen(
                mainUiState = mainUiState,
                mainEventHandler = mainEventHandler,
                homeUiState = uiState,
                homeEventHandler = eventHandler,
                navigateTo = navigateTo,
                onSessionEdit = {},
                bottomBarHeight = bottomBarHeight,
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
                mainUiState = mainUiState,
                mainEventHandler = mainEventHandler,
                homeUiState = uiState,
                homeEventHandler = eventHandler,
                navigateTo = navigateTo,
                timeProvider = timeProvider,
                bottomBarHeight = bottomBarHeight,
            )
        }
        is Screen.HomeTab.Library -> {
            Library(
                mainUiState = mainUiState,
                mainEventHandler = mainEventHandler,
                homeUiState = uiState,
                homeEventHandler = eventHandler,
                navigateTo = navigateTo,
                bottomBarHeight = bottomBarHeight,
            )
        }
        is Screen.HomeTab.Statistics -> {
            Statistics(
                homeUiState = uiState,
                homeEventHandler = eventHandler,
                navigateTo = navigateTo,
                timeProvider = timeProvider,
                bottomBarHeight = bottomBarHeight,
            )
        }
    }
}
