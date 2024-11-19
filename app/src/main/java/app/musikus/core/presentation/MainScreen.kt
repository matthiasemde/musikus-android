/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import app.musikus.activesession.presentation.ActiveSession
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.theme.MusikusTheme
import app.musikus.settings.presentation.addSettingsNavigationGraph
import app.musikus.statistics.presentation.addStatisticsNavigationGraph
import kotlin.reflect.typeOf

@Composable
fun MainScreen(
    timeProvider: TimeProvider,
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val eventHandler = mainViewModel::onUiEvent

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
                timeProvider = timeProvider
            )
        }
    }
}

@Composable
fun MusikusNavHost(
    navController: NavHostController,
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    bottomBarHeight: Dp,
    timeProvider: TimeProvider
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home(tab = HomeTab.default),
        enterTransition = {
            getEnterTransition()
        },
        exitTransition = {
            getExitTransition()
        }
    ) {
        // Home
        composable<Screen.Home>(
            typeMap = mapOf(typeOf<HomeTab>() to HomeTabNavType),
        ) { backStackEntry ->
            val tab = backStackEntry.toRoute<Screen.Home>().tab

            HomeScreen(
                mainUiState = mainUiState,
                mainEventHandler = mainEventHandler,
                bottomBarHeight = bottomBarHeight,
                currentTab = tab,
                navigateTo = { navController.navigate(it) },
                timeProvider = timeProvider
            )
        }

        // Active Session
        composable<Screen.ActiveSession>(
            deepLinks = listOf(
                navDeepLink<Screen.ActiveSession>(
                    basePath = "https://musikus.app"
                )
            )
        ) { backStackEntry ->
            val deepLinkAction = backStackEntry.toRoute<Screen.ActiveSession>().action

            ActiveSession(
                deepLinkAction = deepLinkAction,
                navigateUp = navController::navigateUp,
            )
        }

        // Statistics
        addStatisticsNavigationGraph(
            navController = navController,
        )

        // Settings
        addSettingsNavigationGraph(navController)
    }
}

const val ANIMATION_BASE_DURATION = 400

fun AnimatedContentTransitionScope<NavBackStackEntry>.getEnterTransition(): EnterTransition {
    val initialScreen = initialState.toScreen()
    val targetScreen = targetState.toScreen()

    return when {
        // when changing to active session, slide in from the bottom
        targetScreen is Screen.ActiveSession -> {
            slideInVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseOut),
                initialOffsetY = { fullHeight -> fullHeight }
            )
        }

        // when changing from active session, stay invisible until active session has slid in from the bottom
        initialScreen is Screen.ActiveSession -> {
            fadeIn(
                initialAlpha = 1f,
                animationSpec = tween(durationMillis = ANIMATION_BASE_DURATION)
            )
        }

        // when changing to settings, zoom in when coming from a sub menu
        // and slide in from the right when coming from the home screen
        targetScreen is Screen.Settings -> {
            if (initialScreen is Screen.SettingsOption) {
                scaleIn(
                    animationSpec = tween(ANIMATION_BASE_DURATION / 2),
                    initialScale = 1.2f,
                ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            } else {
                slideInHorizontally(
                    animationSpec = tween(ANIMATION_BASE_DURATION),
                    initialOffsetX = { fullWidth -> (fullWidth / 10) }
                ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            }
        }

        // when changing from settings screen, if going to setting sub menu, zoom out
        // otherwise slide in from the right
        initialScreen is Screen.Settings -> {
            if (targetScreen is Screen.SettingsOption) {
                scaleIn(
                    animationSpec = tween(ANIMATION_BASE_DURATION / 2),
                    initialScale = 0.7f,
                ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            } else {
                slideInHorizontally(
                    animationSpec = tween(ANIMATION_BASE_DURATION),
                    initialOffsetX = { fullWidth -> -(fullWidth / 10) }
                ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            }
        }

        // when changing to session or goal statistics, slide in from the right
        targetScreen is Screen.SessionStatistics ||
            targetScreen is Screen.GoalStatistics -> {
            slideInHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                initialOffsetX = { fullWidth -> (fullWidth / 10) }
            ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // when changing from session or goal statistics, slide in from the left
        initialScreen is Screen.SessionStatistics ||
            initialScreen is Screen.GoalStatistics -> {
            slideInHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                initialOffsetX = { fullWidth -> -(fullWidth / 10) }
            ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // default animation
        else -> {
            slideInVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                initialOffsetY = { fullHeight -> -(fullHeight / 10) }
            ) + fadeIn(
                animationSpec = tween(
                    ANIMATION_BASE_DURATION / 2,
                    ANIMATION_BASE_DURATION / 2
                )
            )
        }
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.getExitTransition(): ExitTransition {
    val initialScreen = initialState.toScreen()
    val targetScreen = targetState.toScreen()

    return when {
        // when changing to active session, show immediately
        targetScreen is Screen.ActiveSession -> {
            fadeOut(tween(durationMillis = 1, delayMillis = ANIMATION_BASE_DURATION))
        }

        // when changing from active session, slide out to the bottom
        initialScreen is Screen.ActiveSession -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseIn),
                targetOffsetY = { fullHeight -> fullHeight }
            )
        }

        // when changing to settings, zoom in when coming from a sub menu
        // and slide out to the left when coming from the home screen
        targetScreen is Screen.Settings -> {
            if (initialScreen is Screen.SettingsOption) {
                scaleOut(
                    animationSpec = tween(ANIMATION_BASE_DURATION / 2),
                    targetScale = 0.7f,
                ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            } else {
                slideOutHorizontally(
                    animationSpec = tween(ANIMATION_BASE_DURATION),
                    targetOffsetX = { fullWidth -> -(fullWidth / 10) }
                ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            }
        }

        // when changing from settings screen, if going to setting sub menu, zoom out
        // otherwise slide out to the right
        initialScreen is Screen.Settings -> {
            if (targetScreen is Screen.SettingsOption) {
                scaleOut(
                    animationSpec = tween(ANIMATION_BASE_DURATION / 2),
                    targetScale = 1.2f,
                ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            } else {
                slideOutHorizontally(
                    animationSpec = tween(ANIMATION_BASE_DURATION),
                    targetOffsetX = { fullWidth -> (fullWidth / 10) }
                ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            }
        }

        // when changing to session or goal statistics, slide in from the right
        targetScreen is Screen.SessionStatistics ||
            targetScreen is Screen.GoalStatistics -> {
            slideOutHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetX = { fullWidth -> (fullWidth / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // when changing from session or goal statistics, slide in from the left
        initialScreen is Screen.SessionStatistics ||
            initialScreen is Screen.GoalStatistics -> {
            slideOutHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetX = { fullWidth -> -(fullWidth / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // default animation
        else -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetY = { fullHeight -> (fullHeight / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }
    }
}
