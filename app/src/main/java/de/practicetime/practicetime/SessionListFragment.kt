package de.practicetime.practicetime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.util.*

class SessionListFragment : Fragment(R.layout.fragment_sessions_list) {

    private var dao: PTDao? = null
    private lateinit var fabNewSession: FloatingActionButton
    private lateinit var fabRunningSession: FloatingActionButton
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        openDatabase()
        createDatabaseFirstRun()

        val sessionListMonths = view.findViewById<RecyclerView>(R.id.sessionList)

        val concatAdapterConfig = ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(false)
            .build()

        val concatAdapter = ConcatAdapter(concatAdapterConfig)

        sessionListMonths.layoutManager = LinearLayoutManager(context)
        sessionListMonths.adapter = concatAdapter

        lifecycleScope.launch {
            // fetch all sessions from the database
            dao?.getSessionsWithSectionsWithCategories()!!.also {
                val sessions = it.toMutableList()

                if (sessions.size == 0) {
                    requireView().apply {
                        findViewById<TextView>(R.id.sessionsHint).visibility = View.VISIBLE
                        findViewById<RecyclerView>(R.id.sessionList).visibility = View.GONE
                    }
                    return@also
                }

                // initialize variables to keep track of the current month
                // and the index of its first session
                var currentMonth: Int
                var firstSessionOfCurrentMonth = 0
                Calendar.getInstance().also { newDate ->
                    newDate.timeInMillis =
                        sessions.first().sections.first().section.timestamp * 1000L
                    currentMonth = newDate.get(Calendar.MONTH)
                }

                var i = 1

                // then loop trough the rest of the sessions...
                while(i < sessions.size) {
                    // ...get the month...
                    var sessionMonth: Int

                    // remove sessions with empty sections list (which should not exist)
                    if(sessions[i].sections.isEmpty()) {
                        sessions.removeAt(i)
                        i--
                    } else {
                        Calendar.getInstance().also { newDate ->
                            newDate.timeInMillis =
                                sessions[i].sections.first().section.timestamp * 1000L
                            sessionMonth = newDate.get(Calendar.MONTH)
                        }

                        // ...and compare it to the current month.
                        // if it is the same, create a new summary adapter
                        // with the respective subList of sessions
                        if (sessionMonth != currentMonth) {
                            concatAdapter.addAdapter(
                                0,
                                SessionSummaryAdapter(
                                    view.context,
                                    false,
                                    sessions.slice(firstSessionOfCurrentMonth until i)
                                )
                            )
                            concatAdapter.notifyItemInserted(0)

                            // set the current month to this sessions month and save its index
                            currentMonth = sessionMonth
                            firstSessionOfCurrentMonth = i
                        }
                    }
                    i++
                }

                // create an adapter for the last subList
                concatAdapter.addAdapter(
                    0,
                    SessionSummaryAdapter(
                        view.context,
                        true,
                        sessions.slice(firstSessionOfCurrentMonth until sessions.size)
                    )
                )
                concatAdapter.notifyItemInserted(0)
            }
        }

        fabNewSession = view.findViewById(R.id.fab_new_session)
        fabRunningSession = view.findViewById(R.id.fab_running_session)
        val clickListener = View.OnClickListener {
            val i = Intent(requireContext(), ActiveSessionActivity::class.java)
            requireActivity().startActivity(i)
            requireActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.fake_anim)
        }
        fabNewSession.setOnClickListener(clickListener)
        fabRunningSession.setOnClickListener(clickListener)
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    private fun createDatabaseFirstRun() {
        lifecycleScope.launch {
            val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

            // FIRST RUN routine
            if (prefs.getBoolean("firstrun", true)) {

                // populate the category table on first run
//                listOf(
//                    Category(name="Die Schöpfung", colorIndex=0),
                    Category(name="Beethoven Septett", colorIndex=1),
                    Category(name="Schostakowitsch 9.", colorIndex=2),
                    Category(name="Trauermarsch c-Moll", colorIndex=3),
                    Category(name="Adagio", colorIndex=4),
                    Category(name="Eine kleine Gigue", colorIndex=5),
                    Category(name="Andantino", colorIndex=6),
                    Category(name="Klaviersonate", colorIndex=7),
                    Category(name="Trauermarsch", colorIndex=8),
//                ).forEach {
//                    dao?.insertCategory(it)
//                }

                prefs.edit().putBoolean("firstrun", false).apply()
            }
        }
    }

    // check if session is running every second to respond the fab design to it
    override fun onResume() {
        super.onResume()
        val fabRunningSession = requireView().findViewById<FloatingActionButton>(R.id.fab_running_session)
        // set correct FAB depending if session is running.
        // wait 100ms and check again to give Service time to stop after discarding session
        runnable = object : Runnable {
            override fun run() {
                if (Singleton.serviceIsRunning) {
                    fabRunningSession.show()
                    fabNewSession.hide()
                } else {
                    fabNewSession.show()
                    fabRunningSession.hide()
                }
                if (Singleton.serviceIsRunning)
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