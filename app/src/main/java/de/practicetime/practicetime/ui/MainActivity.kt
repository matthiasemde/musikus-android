package de.practicetime.practicetime.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.setupWithNavController(navController)
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