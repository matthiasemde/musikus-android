/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import app.musikus.activesession.presentation.ActiveSession
import app.musikus.core.domain.TimeProvider
import app.musikus.core.presentation.components.addMainMenuNavigationGraph
import app.musikus.library.presentation.libraryfolder.LibraryFolderDetailsScreen
import app.musikus.menu.presentation.settings.addSettingsOptionsNavigationGraph
import app.musikus.statistics.presentation.addStatisticsNavigationGraph
import java.util.UUID
import kotlin.reflect.typeOf

@Composable
fun MusikusNavHost(
    navController: NavHostController,
    mainUiState: MainUiState,
    mainEventHandler: MainUiEventHandler,
    bottomBarHeight: Dp,
    timeProvider: TimeProvider,
    isMainMenuOpen: Boolean,
    closeMainMenu: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home(tab = HomeTab.default),
        enterTransition = { getEnterTransition() },
        exitTransition = { getExitTransition() },
        popEnterTransition = { getEnterTransition(pop = true) },
        popExitTransition = { getExitTransition(pop = true) }
    ) {
        // Home
        composable<Screen.Home>(
            typeMap = mapOf(typeOf<HomeTab>() to HomeTabNavType),
        ) { backStackEntry ->
            val tab = backStackEntry.toRoute<Screen.Home>().tab

            BackHandler(
                enabled = isMainMenuOpen,
                onBack = closeMainMenu
            )

            HomeScreen(
                mainUiState = mainUiState,
                mainEventHandler = mainEventHandler,
                bottomBarHeight = bottomBarHeight,
                currentTab = tab,
                navigateTo = { navController.navigate(it) },
                navigateUp = navController::navigateUp,
                timeProvider = timeProvider,
            )
        }

        // Folder Details
        composable<Screen.LibraryFolderDetails> {
            val folderId = UUID.fromString(it.toRoute<Screen.LibraryFolderDetails>().folderId)

            LibraryFolderDetailsScreen(
                mainEventHandler = mainEventHandler,
                folderId = folderId,
                navigateUp = navController::navigateUp
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
        addStatisticsNavigationGraph(navController)

        // Main menu
        addMainMenuNavigationGraph(navController)

        // Settings
        addSettingsOptionsNavigationGraph(navController)
    }
}

const val ANIMATION_BASE_DURATION = 400

fun AnimatedContentTransitionScope<NavBackStackEntry>.getEnterTransition(pop: Boolean = false): EnterTransition {
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

        // when switching between home tabs, do the vertical slide&fade
        initialScreen is Screen.Home && targetScreen is Screen.Home -> {
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

        // when changing to main menu screens, do the horizontal slide&fade (right to left)
        targetScreen is Screen.MainMenuEntry && !pop -> {
            slideInHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                initialOffsetX = { fullWidth -> (fullWidth / 10) }
            ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // when returning from main menu screens, do the horizontal slide&fade (right to left)
        initialScreen is Screen.MainMenuEntry && pop -> {
            slideInHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                initialOffsetX = { fullWidth -> -(fullWidth / 10) }
            ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // default animation: zoom&fade
        else -> {
            if (pop) {
                scaleIn(
                    animationSpec = tween(ANIMATION_BASE_DURATION / 2),
                    initialScale = 1.2f,
                ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            } else {
                scaleIn(
                    animationSpec = tween(ANIMATION_BASE_DURATION / 2),
                    initialScale = 0.7f,
                ) + fadeIn(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            }
        }
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.getExitTransition(pop: Boolean = false): ExitTransition {
    val initialScreen = initialState.toScreen()
    val targetScreen = targetState.toScreen()

    return when {
        // when changing to active session, show immediately
        targetScreen is Screen.ActiveSession -> {
            fadeOut(tween(
                durationMillis = 1,
                delayMillis = ANIMATION_BASE_DURATION
            ))
        }

        // when changing from active session, slide out to the bottom
        initialScreen is Screen.ActiveSession -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION, easing = EaseIn),
                targetOffsetY = { fullHeight -> fullHeight }
            )
        }

        // when switching between home tabs, do the vertical slide&fade
        initialScreen is Screen.Home && targetScreen is Screen.Home -> {
            slideOutVertically(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetY = { fullHeight -> (fullHeight / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // when changing to main menu screens, do the horizontal slide&fade (right to left)
        targetScreen is Screen.MainMenuEntry && !pop -> {
            slideOutHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetX = { fullWidth -> -(fullWidth / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // when returning from main menu screens, do the horizontal slide&fade (right to left)
        initialScreen is Screen.MainMenuEntry && pop -> {
            slideOutHorizontally(
                animationSpec = tween(ANIMATION_BASE_DURATION),
                targetOffsetX = { fullWidth -> (fullWidth / 10) }
            ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
        }

        // default animation: zoom&fade
        else -> {
            if(pop) {
                scaleOut(
                    animationSpec = tween(ANIMATION_BASE_DURATION / 2),
                    targetScale = 0.7f,
                ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            } else {
                scaleOut(
                    animationSpec = tween(ANIMATION_BASE_DURATION / 2),
                    targetScale = 1.2f,
                ) + fadeOut(animationSpec = tween(ANIMATION_BASE_DURATION / 2))
            }
        }
    }
}
