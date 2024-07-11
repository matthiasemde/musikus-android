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
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import app.musikus.activesession.presentation.ActiveSession
import app.musikus.sessionslist.presentation.EditSession
import app.musikus.settings.presentation.addSettingsNavigationGraph
import app.musikus.statistics.presentation.addStatisticsNavigationGraph
import app.musikus.core.presentation.theme.MusikusTheme
import app.musikus.core.domain.TimeProvider
import java.util.UUID

const val DEEP_LINK_KEY = "argument"

@Composable
fun MusikusApp(
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


    MusikusTheme(
        theme = theme,
        colorScheme = colorScheme
    ) {

        NavHost(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                getEnterTransition()
            },
            exitTransition = {
                getExitTransition()
            }
        ) {
            navigation(
                route = Screen.Home.route,
                startDestination = Screen.HomeTab.defaultTab.route
            ) {
                composable(
                    route = "home/{tab}",
                    arguments = listOf(navArgument("tab") {
                        nullable = true
                    })
                ) { backStackEntry ->
                    val tabRoute = backStackEntry.arguments?.getString("tab")
                    val tab = Screen.HomeTab.allTabs.firstOrNull { it.subRoute == tabRoute }

                    HomeScreen(
                        mainUiState = uiState,
                        mainEventHandler = eventHandler,
                        initialTab = tab,
                        navController = navController,
                        timeProvider = timeProvider
                    )
                }
            }

            composable(
                route = Screen.EditSession.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType})
            ) {backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                    ?: return@composable navController.navigate(Screen.HomeTab.Sessions.route)

                EditSession(
                    sessionToEditId = UUID.fromString(sessionId),
                    navigateUp = navController::navigateUp
                )
            }
            composable(
                route = Screen.ActiveSession.route,
                deepLinks = listOf(navDeepLink {
                    uriPattern = "musikus://activeSession/{$DEEP_LINK_KEY}"
                })
            ) { backStackEntry ->
                ActiveSession(
                    navigateUp = navController::navigateUp,
                    deepLinkArgument = backStackEntry.arguments?.getString(DEEP_LINK_KEY),
                    navigateTo = navController::navigateTo
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
}

fun NavController.navigateTo(screen: Screen) {
    navigate(screen.route)
}


const val ANIMATION_BASE_DURATION = 400

fun AnimatedContentTransitionScope<NavBackStackEntry>.getEnterTransition() : EnterTransition {
    val initialRoute = initialState.destination.route ?: return fadeIn()
    val targetRoute = targetState.destination.route ?: return fadeIn()

    return when {
        // when changing to active session, slide in from the bottom
        targetRoute == Screen.ActiveSession.route -> {
            slideInVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseOut),
                initialOffsetY = { fullHeight -> fullHeight }
            )
        }

        // when changing from active session, stay invisible until active session has slid in from the bottom
        initialRoute == Screen.ActiveSession.route -> {
            fadeIn(
                initialAlpha = 1f,
                animationSpec = tween(durationMillis = ANIMATION_BASE_DURATION)
            )
        }

        // when changing to settings, zoom in when coming from a sub menu
        // and slide in from the right when coming from the home screen
        targetRoute == Screen.Settings.route -> {
            if(initialRoute in (Screen.SettingsOption.allSettings.map { it.route }) ) {
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
        initialRoute == Screen.Settings.route -> {
            if (targetRoute in (Screen.SettingsOption.allSettings.map { it.route })) {
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
        targetRoute == Screen.SessionStatistics.route ||
        targetRoute == Screen.GoalStatistics.route -> {
            slideInHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                initialOffsetX = { fullWidth -> (fullWidth / 10) }
            ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // when changing from session or goal statistics, slide in from the left
        initialRoute == Screen.SessionStatistics.route ||
        initialRoute == Screen.GoalStatistics.route -> {
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

fun AnimatedContentTransitionScope<NavBackStackEntry>.getExitTransition() : ExitTransition {
    val initialRoute = initialState.destination.route ?: return fadeOut()
    val targetRoute = targetState.destination.route ?: return fadeOut()

    return when {
        // when changing to active session, show immediately
        targetRoute == Screen.ActiveSession.route -> {
            fadeOut(tween(durationMillis = 1, delayMillis = ANIMATION_BASE_DURATION))
        }

        // when changing from active session, slide out to the bottom
        initialRoute == Screen.ActiveSession.route -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseIn),
                targetOffsetY = { fullHeight -> fullHeight }
            )
        }

        // when changing to settings, zoom in when coming from a sub menu
        // and slide out to the left when coming from the home screen
        targetRoute == Screen.Settings.route -> {
            if(initialRoute in (Screen.SettingsOption.allSettings.map { it.route })) {
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
        initialRoute == Screen.Settings.route -> {
            if (targetRoute in (Screen.SettingsOption.allSettings.map { it.route })) {
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
        targetRoute == Screen.SessionStatistics.route ||
        targetRoute == Screen.GoalStatistics.route -> {
            slideOutHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetX = { fullWidth -> (fullWidth / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // when changing from session or goal statistics, slide in from the left
        initialRoute == Screen.SessionStatistics.route ||
        initialRoute == Screen.GoalStatistics.route -> {
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