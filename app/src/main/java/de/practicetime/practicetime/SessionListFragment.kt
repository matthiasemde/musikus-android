package de.practicetime.practicetime

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.launch
import java.time.Period
import java.util.*


private var dao: PTDao? = null

class SessionListFragment : Fragment(R.layout.fragment_sessions_list) {
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
            dao?.getSessionsWithSectionsWithCategories()!!.also { it ->
                var sessions = it.toMutableList()

                if (sessions.size == 0) return@also

                // initialize variables to keep track of the current month
                // and the index of its first session
                var currentMonth: Int
                var firstSessionOfCurrentMonth: Int = 0
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

        val fab = view.findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val i = Intent(requireContext(), ActiveSessionActivity::class.java)
            requireActivity().startActivity(i)
            requireActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.fake_anim)
        }
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
                    dao?.insertCategory(it)
                }

                listOf(
                    Goal(startTimestamp = Date().time / 1000L, type = GoalType.SPECIFIC_CATEGORIES, period = 100000, periodUnit = GoalPeriodUnit.WEEK, target = 50),
                    Goal(startTimestamp = Date().time / 1000L, type = GoalType.SPECIFIC_CATEGORIES, period = 100000, periodUnit = GoalPeriodUnit.WEEK, target = 20),
                    Goal(startTimestamp = Date().time / 1000L, type = GoalType.SPECIFIC_CATEGORIES, period = 200000, periodUnit = GoalPeriodUnit.WEEK, target = 70),
                ).forEach {
                    dao?.insertGoal(it)
                }

                listOf(
                    GoalCategoryCrossRef(goalId = 1, categoryId = 1),
                    GoalCategoryCrossRef(goalId = 2, categoryId = 2),
                    GoalCategoryCrossRef(goalId = 3, categoryId = 3),
                ).forEach {
                    dao?.insertGoalCategoryCrossRef(it)
                }


                prefs.edit().putBoolean("firstrun", false).apply();
            }
        }
    }
}