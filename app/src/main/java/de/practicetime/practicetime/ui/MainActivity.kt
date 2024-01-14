/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 */

package de.practicetime.practicetime.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.practicetime.practicetime.BuildConfig
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.ui.intro.AppIntroActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(getString(R.string.filename_shared_preferences), Context.MODE_PRIVATE)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.setupWithNavController(navController)

        if (BuildConfig.DEBUG) {
            createDatabaseFirstRun()
        }
        setTheme()
    }

    private fun launchAppIntro() {
        val i = Intent(this, AppIntroActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(i)
    }

    private fun announceUpdate(
        @LayoutRes layout: Int,
        onConfirm: () -> Unit
    ) {
        val builder = AlertDialog.Builder(this).apply {
            setCancelable(false)
            setView(this@MainActivity.layoutInflater.inflate(layout, null))
            setPositiveButton("Awesome!") { _, _ -> onConfirm() }
        }
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.dialog_background)
        )
        dialog.show()
    }

    private fun createDatabaseFirstRun() {
        lifecycleScope.launch {

            // FIRST RUN routine
            if (prefs.getBoolean(PracticeTime.PREFERENCES_KEY_FIRSTRUN, true)) {

                // populate the category table on first run
                listOf(
                    Category(name="Die Schöpfung", colorIndex=0),
                    Category(name="Beethoven Septett", colorIndex=1),
                    Category(name="Schostakowitsch 9.", colorIndex=2),
                    Category(name="Trauermarsch c-Moll", colorIndex=3),
                    Category(name="Adagio", colorIndex=4),
                    Category(name="Eine kleine Gigue", colorIndex=5),
                    Category(name="Andantino", colorIndex=6),
                    Category(name="Klaviersonate", colorIndex=7),
                    Category(name="Trauermarsch", colorIndex=8),
                ).forEach {
                    PracticeTime.categoryDao.insert(it)
                }

                prefs.edit().putBoolean(PracticeTime.PREFERENCES_KEY_FIRSTRUN, false).apply()
            }
        }
    }

    private fun setTheme() {
        val chosenTheme = prefs.getInt(PracticeTime.PREFERENCES_KEY_THEME, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
        AppCompatDelegate.setDefaultNightMode(chosenTheme)
    }

    // periodically check if session is still running (if it is) to remove the badge if yes
    override fun onResume() {
        super.onResume()

        if (!BuildConfig.DEBUG) {
            if (!prefs.getBoolean(PracticeTime.PREFERENCES_KEY_APPINTRO_DONE, false)) {
                // make sure the update message is not shown after app intro
                prefs.edit().putBoolean(PracticeTime.PREFERENCES_KEY_UPDATE_1_1_0, true).apply()

                launchAppIntro()
            }
            if (!prefs.getBoolean(PracticeTime.PREFERENCES_KEY_UPDATE_1_1_0, false)) {
                announceUpdate(layout = R.layout.dialog_announce_update_1_1_0) {
                    prefs.edit().putBoolean(PracticeTime.PREFERENCES_KEY_UPDATE_1_1_0, true).apply()
                }
            }
        }

        runnable = object : Runnable {
            override fun run() {
                if (PracticeTime.serviceIsRunning) {
                    bottomNavigationView.getOrCreateBadge(R.id.sessionListFragment)
                } else {
                    bottomNavigationView.removeBadge(R.id.sessionListFragment)
                }
                if (PracticeTime.serviceIsRunning)
                    handler.postDelayed(this, 1000)
            }
        }
        handler = Handler(Looper.getMainLooper()).also {
            it.post(runnable)
        }
    }

    // remove the callback. Otherwise, the runnable will keep going and when entering the activity again,
    // there will be twice as much and so on...
    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
    }
}
