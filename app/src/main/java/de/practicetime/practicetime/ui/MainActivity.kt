package de.practicetime.practicetime.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.setupWithNavController(navController)

        createDatabaseFirstRun()
    }

    private fun createDatabaseFirstRun() {
        lifecycleScope.launch {
            prefs = getPreferences(Context.MODE_PRIVATE)

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
                    PracticeTime.dao.insertCategory(it)
                }

                prefs.edit().putBoolean(PracticeTime.PREFERENCES_KEY_FIRSTRUN, false).apply()
            }

            setTheme()
        }
    }

    private fun setTheme() {
        val chosenTheme = prefs.getInt(PracticeTime.PREFERENCES_KEY_THEME, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
        AppCompatDelegate.setDefaultNightMode(chosenTheme)
    }

    // periodically check if session is still running (if it is) to remove the badge if yes
    override fun onResume() {
        super.onResume()
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