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
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.material.composethemeadapter3.Mdc3Theme
import de.practicetime.practicetime.BuildConfig
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import de.practicetime.practicetime.ui.intro.AppIntroActivity
import de.practicetime.practicetime.ui.library.LibraryComposable
import de.practicetime.practicetime.ui.sessionlist.SessionListFragmentHolder
import kotlinx.coroutines.launch

class MainState() {
//    val navController = NavController()
}

@Composable
fun rememberMainState() = remember() { MainState() }

sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object Sessions : Screen("sessionList", R.string.navigationSessionsTitle)
    object Goals : Screen("goals", R.string.navigationGoalsTitle)
    object Statistics : Screen("statisticsOverview", R.string.navigationStatisticsTitle)
    object Library : Screen("library", R.string.navigationLibraryTitle)
}

class MainActivity : FragmentActivity() {

//    private lateinit var bottomNavigationView: BottomNavigationView
//    private lateinit var handler: Handler
//    private lateinit var runnable: Runnable

    private val navItems = listOf(
        Screen.Sessions,
//        Screen.Goals,
//        Screen.Statistics,
        Screen.Library
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Mdc3Theme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            navItems.forEach { screen ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text(stringResource(screen.resourceId)) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
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
                        startDestination = Screen.Sessions.route,
                        Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Sessions.route) { SessionListFragmentHolder() }
                        composable(Screen.Library.route) { LibraryComposable() }
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
//        setTheme()

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
