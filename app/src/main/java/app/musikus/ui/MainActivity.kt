/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package app.musikus.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import app.musikus.BuildConfig
import app.musikus.Musikus
import app.musikus.utils.ExportDatabaseContract
import app.musikus.utils.ImportDatabaseContract
import app.musikus.utils.PermissionChecker
import app.musikus.utils.PermissionCheckerActivity
import app.musikus.utils.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : PermissionCheckerActivity() {

    @Inject
    lateinit var application: Musikus

    @Inject
    override lateinit var permissionChecker: PermissionChecker

    @Inject
    lateinit var timeProvider: TimeProvider


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
//            launchAppIntroFirstRun()
        }

        Musikus.exportLauncher = registerForActivityResult(
            ExportDatabaseContract()
        ) { application.exportDatabaseCallback(applicationContext, it) }

        Musikus.importLauncher = registerForActivityResult(
            ImportDatabaseContract()
        ) { application.importDatabaseCallback(applicationContext, it) }

        requestRuntimePermissions()

        setContent {
            MusikusApp(timeProvider)
        }
    }

    private fun requestRuntimePermissions() {
        // TODO: handle if user rejects permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
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
