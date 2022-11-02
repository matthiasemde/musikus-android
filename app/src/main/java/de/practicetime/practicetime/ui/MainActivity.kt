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

package de.practicetime.practicetime.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.material.composethemeadapter3.Mdc3Theme
import de.practicetime.practicetime.BuildConfig
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.ui.goals.GoalsFragmentHolder
import de.practicetime.practicetime.ui.intro.AppIntroActivity
import de.practicetime.practicetime.ui.library.LibraryComposable
import de.practicetime.practicetime.ui.sessionlist.SessionListFragmentHolder
import de.practicetime.practicetime.ui.statistics.StatisticsFragmentHolder
import kotlinx.coroutines.launch

class MainState() {
    val showNavBarScrim = mutableStateOf(false)
}

@Composable
fun rememberMainState() = remember() { MainState() }

sealed class Screen(val route: String, @StringRes val resourceId: Int, @DrawableRes val icon: Int) {
    object Sessions : Screen("sessionList", R.string.navigationSessionsTitle, R.drawable.avd_sessions)
    object Goals : Screen("goals", R.string.navigationGoalsTitle, R.drawable.avd_goals)
    object Statistics : Screen("statisticsOverview", R.string.navigationStatisticsTitle, R.drawable.avd_bar_chart)
    object Library : Screen("library", R.string.navigationLibraryTitle, R.drawable.avd_library)
}

class MainActivity : AppCompatActivity() {

    private val navItems = listOf(
        Screen.Sessions,
        Screen.Goals,
        Screen.Statistics,
        Screen.Library
    )

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationGraphicsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)


        setTheme()

        setContent {
            val mainState = rememberMainState()
            Mdc3Theme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            navItems.forEach { screen ->
                                var atEnd by remember { mutableStateOf(false) }
                                var icon by remember { mutableStateOf(screen.icon) }
                                NavigationBarItem(
                                    icon = {
                                        val image = AnimatedImageVector.animatedVectorResource(icon)
                                        Image(
                                            painter = rememberAnimatedVectorPainter(
                                                animatedImageVector = image,
                                                atEnd = atEnd
                                            ),
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text(stringResource(screen.resourceId)) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        atEnd = true
                                        lifecycleScope.launch {
                                            icon = screen.icon
                                        }
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
                    }
                ) { innerPadding ->
                    NavHost(
                        navController,
                        startDestination = Screen.Library.route,
                        Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        composable(Screen.Sessions.route) { SessionListFragmentHolder() }
                        composable(Screen.Goals.route) { GoalsFragmentHolder() }
                        composable(Screen.Statistics.route) { StatisticsFragmentHolder() }
                        composable(Screen.Library.route) { LibraryComposable (
                            contentPadding = innerPadding,
                            mainState = mainState,
                            showNavBarScrim = { show -> mainState.showNavBarScrim.value = show }
                        ) }
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .zIndex(1f),
                        visible = mainState.showNavBarScrim.value,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(innerPadding.calculateBottomPadding())
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {}
                            )
                        }
                    }
                }
            }
        }
    }
//        setContentView(R.layout.activity_main)

//        val navController = rememberNavController()
//
//        val navHostFragment =
//            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        val navController = navHostFragment.navController
//
//        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
//        bottomNavigationView.setupWithNavController(navController)
//
//        if (BuildConfig.DEBUG) {
//            createDatabaseFirstRun()
////            launchAppIntroFirstRun()
//        }

    private fun launchAppIntroFirstRun() {
        if (!PracticeTime.prefs.getBoolean(PracticeTime.PREFERENCES_KEY_APPINTRO_DONE, false)) {
            val i = Intent(this, AppIntroActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(i)
        }
    }

    private fun createDatabaseFirstRun() {
        lifecycleScope.launch {

            // FIRST RUN routine
            if (PracticeTime.prefs.getBoolean(PracticeTime.PREFERENCES_KEY_FIRSTRUN, true)) {

                listOf(
                    LibraryFolder(name="Schupra"),
                    LibraryFolder(name="Fagott"),
                    LibraryFolder(name="Gesang"),
                ).forEach {
                    PracticeTime.libraryFolderDao.insert(it)
                }

                // populate the libraryItem table on first run
                listOf(
                    LibraryItem(name="Die Schöpfung", colorIndex=0, libraryFolderId = 1),
                    LibraryItem(name="Beethoven Septett", colorIndex=1, libraryFolderId = 1),
                    LibraryItem(name="Schostakowitsch 9.", colorIndex=2, libraryFolderId = 2),
                    LibraryItem(name="Trauermarsch c-Moll", colorIndex=3, libraryFolderId = 2),
                    LibraryItem(name="Adagio", colorIndex=4, libraryFolderId = 3),
                    LibraryItem(name="Eine kleine Gigue", colorIndex=5, libraryFolderId = 3),
                    LibraryItem(name="Andantino", colorIndex=6),
                    LibraryItem(name="Klaviersonate", colorIndex=7),
                    LibraryItem(name="Trauermarsch", colorIndex=8),
                ).forEach {
                    PracticeTime.libraryItemDao.insert(it)
                }

                PracticeTime.prefs.edit().putBoolean(PracticeTime.PREFERENCES_KEY_FIRSTRUN, false).apply()
            }
        }
    }

    private fun setTheme() {
        val chosenTheme = PracticeTime.prefs.getInt(PracticeTime.PREFERENCES_KEY_THEME, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
        AppCompatDelegate.setDefaultNightMode(chosenTheme)
    }

    // periodically check if session is still running (if it is) to remove the badge if yes
    override fun onResume() {
        super.onResume()
        if (!BuildConfig.DEBUG)
            launchAppIntroFirstRun()
//        runnable = object : Runnable {
//            override fun run() {
//                if (PracticeTime.serviceIsRunning) {
//                    bottomNavigationView.getOrCreateBadge(R.id.sessionListFragment)
//                } else {
//                    bottomNavigationView.removeBadge(R.id.sessionListFragment)
//                }
//                if (PracticeTime.serviceIsRunning)
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
