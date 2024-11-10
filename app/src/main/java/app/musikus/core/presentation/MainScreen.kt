/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.components.MainMenu
import app.musikus.core.presentation.theme.MusikusTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    timeProvider: TimeProvider,
    viewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = viewModel::onUiEvent

    // This line ensures, that the app is only drawn when the proper theme is loaded
    // TODO: make sure this is the right way to do it
    val theme = uiState.activeTheme ?: return
    val colorScheme = uiState.activeColorScheme ?: return

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember {
        derivedStateOf {
            navBackStackEntry?.toScreen()
        }
    }
    val currentTab by remember {
        derivedStateOf {
            currentRoute?.let { if (it is Screen.Home) it.tab else null }
        }
    }

    MusikusTheme(
        theme = theme,
        colorScheme = colorScheme
    ) {
        val scope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        val lifeCycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(viewModel.eventChannel, lifeCycleOwner.lifecycle) {
            lifeCycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventChannel.collect {
                    when (it) {
                        is MainEvent.OpenMainDrawer -> drawerState.open()
                    }
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    MainMenu(
                        navigateTo = { navController.navigate(it) },
                        onDismiss = { scope.launch { drawerState.close() } }
                    )
                }
            },
            gesturesEnabled = drawerState.isOpen, // only allow gestures to close the drawer once it is open
        ) {
            // This is the main scaffold of the app which contains the bottom navigation,
            // the snackbar host and the nav host
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = uiState.snackbarHost)
                },
                bottomBar = {
                    MusikusBottomBar(
                        mainUiState = uiState,
                        mainEventHandler = eventHandler,
                        currentTab = currentTab,
                        onTabSelected = { selectedTab ->
                            navController.navigate(Screen.Home(selectedTab)) {
                                popUpTo(Screen.Home(HomeTab.default)) {
                                    inclusive = selectedTab == HomeTab.default
                                }
                            }
                        },
                    )
                }
            ) { innerPadding ->

                // Calculate the height of the bottom bar so we can add it as  padding in the home tabs
                val bottomBarHeight = innerPadding.calculateBottomPadding()

                MusikusNavHost(
                    navController = navController,
                    mainUiState = uiState,
                    mainEventHandler = eventHandler,
                    bottomBarHeight = bottomBarHeight,
                    timeProvider = timeProvider,
                    isMainMenuOpen = drawerState.isOpen,
                    closeMainMenu = { scope.launch { drawerState.close() } }
                )
            }
        }
    }
}
