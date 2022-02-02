package de.practicetime.practicetime.ui

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import de.practicetime.practicetime.BuildConfig
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category
import de.practicetime.practicetime.services.SessionForegroundService
import de.practicetime.practicetime.ui.activesession.ActiveSessionActivity
import de.practicetime.practicetime.utils.TIME_FORMAT_HMS_DIGITAL
import de.practicetime.practicetime.utils.getDurationString
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var prefs: SharedPreferences
    private lateinit var nowPlayingCard: MaterialCardView

    private lateinit var mService: SessionForegroundService
    private var mBound: Boolean = false
    private var blinkTime = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        /** called by service when we have connection to the service => we have mService reference */
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to SessionForegroundService, cast the IBinder and get SessionForegroundService instance
            val binder = service as SessionForegroundService.LocalBinder
            mService = binder.getService()
            mBound = true

            // show the now playing card
            initNowPlayingCard()
            updateNowPlayingCard()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }


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

    // called when service is connected
    private fun initNowPlayingCard() {
        nowPlayingCard.visibility = View.VISIBLE

        // Play/Pause Button
        findViewById<ImageButton>(R.id.activtiy_main_btn_now_playing).setOnClickListener {
            if (mService.paused) {
                mService.paused = false
                mService.pauseDuration = 0
            } else {
                mService.paused = true
            }
            updateNowPlayingCard()
        }

        nowPlayingCard.setOnClickListener {
            val i = Intent(this, ActiveSessionActivity::class.java)
            startActivity(i)
            overridePendingTransition(R.anim.slide_in_up, R.anim.fake_anim)
        }
    }

    private fun updateNowPlayingCard() {
        val tvCat = findViewById<TextView>(R.id.activity_main_tv_now_playing_category)
        val tvTime = findViewById<TextView>(R.id.activity_main_tv_now_playing_time)
        val playPause = findViewById<ImageButton>(R.id.activtiy_main_btn_now_playing)

        tvCat.text = mService.currCategoryName
        tvTime.text = getDurationString(mService.totalPracticeDuration, TIME_FORMAT_HMS_DIGITAL)

        if (mService.paused) {
            playPause.setImageResource(R.drawable.ic_play)
            tvTime.visibility =
                if (blinkTime)
                    View.INVISIBLE
                else
                    View.VISIBLE
            blinkTime = !blinkTime
        } else {
            playPause.setImageResource(R.drawable.ic_pause)
            tvTime.visibility = View.VISIBLE
        }
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
                    if (BuildConfig.DEBUG)
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
        nowPlayingCard = findViewById(R.id.activity_main_card_now_playing)
        runnable = object : Runnable {
            override fun run() {
                if (PracticeTime.serviceIsRunning) {
                    bottomNavigationView.getOrCreateBadge(R.id.sessionListFragment)

                    if (!nowPlayingCard.isVisible) {
                        // Bind to SessionForegroundService
                        Intent(this@MainActivity, SessionForegroundService::class.java).also { intent ->
                            bindService(intent, connection, Context.BIND_AUTO_CREATE)
                        }
                    } else {
                        updateNowPlayingCard()
                    }
                } else {
                    bottomNavigationView.removeBadge(R.id.sessionListFragment)
                    if (nowPlayingCard.isVisible)
                        nowPlayingCard.visibility = View.GONE
                }
                if (PracticeTime.serviceIsRunning)
                    handler.postDelayed(this, 500)
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
        if (mBound) {
            unbindService(connection)
            mBound = false
        }
    }
}