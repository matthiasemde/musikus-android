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
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.view.WindowCompat
import app.musikus.BuildConfig
import app.musikus.Musikus
import app.musikus.utils.ExportDatabaseContract
import app.musikus.utils.ImportDatabaseContract
import app.musikus.utils.TimeProvider
import com.google.android.material.composethemeadapter3.Mdc3Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject



@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var timeProvider: TimeProvider

    private val navItems = listOf(
        Screen.Sessions,
        Screen.Goals,
        Screen.Statistics,
        Screen.Library
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
            Mdc3Theme {
                MusikusApp(timeProvider)
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
