/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package app.musikus.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.musikus.BuildConfig
import app.musikus.Musikus
import app.musikus.R
import app.musikus.getActivity
import app.musikus.shared.MultiFabState
import app.musikus.ui.goals.Goals
import app.musikus.ui.goals.ProgressUpdate
import app.musikus.ui.library.Library
import app.musikus.ui.sessionlist.EditSession
import app.musikus.ui.sessionlist.Sessions
import app.musikus.ui.statistics.Statistics
import app.musikus.ui.statistics.sessionstatistics.SessionStatistics
import app.musikus.utils.ExportDatabaseContract
import app.musikus.utils.ExportImportDialog
import app.musikus.utils.ImportDatabaseContract
import app.musikus.viewmodel.MainViewModel
import com.google.android.material.composethemeadapter3.Mdc3Theme
import java.util.UUID


sealed class Screen(
    val route: String,
    val navigationBarData: NavigationBarData? = null
) {
    data object Sessions : Screen(
        route = "sessionList",
        NavigationBarData(
            title = R.string.navigationSessionsTitle,
            staticIcon = R.drawable.ic_sessions,
            animatedIcon = R.drawable.avd_sessions,
        )
    )

    data object EditSession : Screen(
        route = "editSession/{sessionId}",
    )
    data object Goals : Screen(
        route = "goals",
        NavigationBarData(
            title = R.string.navigationGoalsTitle,
            staticIcon = R.drawable.ic_goals,
            animatedIcon = R.drawable.avd_goals
        )
    )
    data object Statistics : Screen(
        route = "statistics",
        NavigationBarData(
            title = R.string.navigationStatisticsTitle,
            staticIcon = R.drawable.ic_bar_chart,
            animatedIcon = R.drawable.avd_bar_chart
        )
    )
    data object SessionStatistics : Screen(
        route = "sessionStatistics",
    )
    data object Library : Screen(
        route = "library",
        NavigationBarData(
            title = R.string.navigationLibraryTitle,
            staticIcon = R.drawable.ic_library,
            animatedIcon = R.drawable.avd_library
        )
    )
    data object ProgressUpdate : Screen(
        route = "progressUpdate",
    )

    data class NavigationBarData(
        @StringRes val title: Int,
        @DrawableRes val staticIcon: Int,
        @DrawableRes val animatedIcon: Int
    )
}

class MainActivity : AppCompatActivity() {

    private val navItems = listOf(
        Screen.Sessions,
        Screen.Goals,
        Screen.Statistics,
        Screen.Library
    )

    @OptIn(
        ExperimentalAnimationGraphicsApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (BuildConfig.DEBUG) {
//            launchAppIntroFirstRun()
        }

        val reloadDatabase = mutableIntStateOf(0)

        Musikus.exportLauncher = registerForActivityResult(
            ExportDatabaseContract()
        ) { Musikus.exportDatabaseCallback(applicationContext, it) }

        Musikus.importLauncher = registerForActivityResult(
            ImportDatabaseContract()
        ) {
            Musikus.importDatabaseCallback(applicationContext, it)
            reloadDatabase.intValue++
        }

        setContent {

            val mainViewModel: MainViewModel = viewModel()
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            val navController = rememberNavController()

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showNavigationBar = currentDestination?.hierarchy?.any { dest ->
                navItems.any { it.route == dest.route }
            } == true

            Mdc3Theme {
                Log.d("MainActivity", "${LocalViewModelStoreOwner.current}")
                Scaffold(
                    snackbarHost = {
                        val hostState by mainViewModel.snackbarHostState.collectAsStateWithLifecycle()
                        SnackbarHost(hostState = hostState)
                   },
                    bottomBar = {
                        if (showNavigationBar) {
                            Box {
                                NavigationBar {
                                    navItems.forEach { screen ->
                                        val selected =
                                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                        val painterCount = 5
                                        var activePainter by remember { mutableIntStateOf(0) }
                                        val painter = rememberVectorPainter(
                                            image = ImageVector.vectorResource(screen.navigationBarData!!.staticIcon)
                                        )
                                        val animatedPainters = (0..painterCount).map {
                                            rememberAnimatedVectorPainter(
                                                animatedImageVector = AnimatedImageVector.animatedVectorResource(
                                                    screen.navigationBarData.animatedIcon
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
                                                    text = stringResource(screen.navigationBarData.title),
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            selected = selected,
                                            onClick = {
                                                if (!selected) activePainter =
                                                    (activePainter + 1) % painterCount
                                                navController.navigate(screen.route) {
                                                    // Pop up to the start destination of the graph to
                                                    // avoid building up a large stack of destinations
                                                    // on the back stack as users select items
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    // Avoid multiple copies of the same destination when
                                                    // reselecting the same item
                                                    launchSingleTop = true
                                                    // Restore state when reselecting a previously selected item
                                                    restoreState = true
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
                                    visible = uiState.multiFabState == MultiFabState.EXPANDED,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = mainViewModel::collapseMultiFab
                                            )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Log.d("MainActivity", "innerPadding($currentDestination): $innerPadding")
                    val animationDuration = 400
                    NavHost(
                        navController,
                        startDestination = Screen.Sessions.route,
                        Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                        enterTransition = {
                            slideInVertically(
                                animationSpec = tween(animationDuration),
                                initialOffsetY = { fullHeight -> -(fullHeight / 10) }
                            ) + fadeIn(
                                animationSpec = tween(
                                    animationDuration / 2,
                                    animationDuration / 2
                                )
                            )
                        },
                        exitTransition = {
                            slideOutVertically(
                                animationSpec = tween(animationDuration),
                                targetOffsetY = { fullHeight -> (fullHeight / 10) }
                            ) + fadeOut(animationSpec = tween(animationDuration / 2))
                        }
                    ) {
                        composable(
                            route = Screen.Sessions.route,
                            exitTransition = {
                                if(initialState.destination.route == Screen.ProgressUpdate.route) {
                                    fadeOut(tween(0))
                                } else null
                            }
                        ) {
                            Sessions(mainViewModel, getActivity()) { sessionId: UUID ->
                                navController.navigate(
                                    Screen.EditSession.route.replace(
                                        "{sessionId}",
                                        sessionId.toString()
                                    )
                                )
                            }
                        }
                        composable(
                            route = Screen.Goals.route,
                        ) { Goals(mainViewModel) }
                        composable(
                            route = Screen.Statistics.route,
                        ) { Statistics(
                            mainViewModel = mainViewModel,
                            navigateToSessionStatistics = {
                                navController.navigate(Screen.SessionStatistics.route)
                            }
                        ) }
                        composable(
                            route = Screen.SessionStatistics.route,
                        ) { SessionStatistics(
                            navigateUp = {
                                navController.navigateUp()
                            }
                        ) }
                        composable(
                            route = Screen.Library.route,
                        ) { Library (mainViewModel) }
                        composable(
                            route = Screen.ProgressUpdate.route,
                            enterTransition = { fadeIn(tween(0)) }
                        ) { ProgressUpdate(mainViewModel) }
                        composable(
                            route = Screen.EditSession.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType})
                        ) {backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getString("sessionId")
                                ?: return@composable navController.navigate(Screen.Sessions.route)

                            EditSession(sessionToEditId = UUID.fromString(sessionId))
                        }
                    }

                    /** Export / Import Dialog */
                    ExportImportDialog(
                        show = uiState.showExportImportDialog,
                        onDismissHandler = mainViewModel::hideExportImportDialog
                    )

                    // if there is a new session added to the intent, navigate to progress update
                    intent.extras?.getLong("KEY_SESSION")?.let { newSessionId ->
                        intent.removeExtra("KEY_SESSION")
//                        mainViewModel.navigateTo(Screen.ProgressUpdate.route)
                    }
                }
            }
        }
    }

    private fun launchAppIntroFirstRun() {
//        if (!Musikus.prefs.getBoolean(Musikus.PREFERENCES_KEY_APPINTRO_DONE, false)) {
//            val i = Intent(this, AppIntroActivity::class.java)
//            i.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
//            startActivity(i)
//        }
    }

    // periodically check if session is still running (if it is) to remove the badge if yes
    override fun onResume() {
        super.onResume()
        if (!BuildConfig.DEBUG)
            launchAppIntroFirstRun()
//        runnable = object : Runnable {
//            override fun run() {
//                if (Musikus.serviceIsRunning) {
//                    bottomNavigationView.getOrCreateBadge(R.id.sessionListFragment)
//                } else {
//                    bottomNavigationView.removeBadge(R.id.sessionListFragment)
//                }
//                if (Musikus.serviceIsRunning)
//                    handler.postDelayed(this, 1000)
//            }
//        }
//        handler = Handler(Looper.getMainLooper()).also {
//            it.post(runnable)
//        }
    }

    // remove the callback. Otherwise, the runnable will keep going and when entering the activity again,
    // there will be twice as much and so on...
//    override fun onStop() {
//        super.onStop()
//        handler.removeCallbacks(runnable)
//    }
}
