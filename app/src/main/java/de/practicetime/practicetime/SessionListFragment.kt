package de.practicetime.practicetime

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.SessionWithSectionsWithCategories
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


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
            dao?.getSessionsWithSectionsWithCategories()!!.also { sessions ->

                // initialize variables to keep track of the current month
                // and the index of its first session
                var currentMonth: Int
                var firstSessionOfCurrentMonth: Int = 0
                Calendar.getInstance().also { newDate ->
                    newDate.timeInMillis =
                        sessions.first().sections.first().section.timestamp * 1000L
                    currentMonth = newDate.get(Calendar.DAY_OF_MONTH)
                }

                // then loop trough the rest of the sessions...
                for(i in 1 until sessions.size) {
                    // ...get the month...
                    var sessionMonth: Int
                    Calendar.getInstance().also { newDate ->
                        newDate.timeInMillis =
                            sessions[i].sections.first().section.timestamp * 1000L
                        sessionMonth = newDate.get(Calendar.DAY_OF_MONTH)
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
                                sessions.subList(firstSessionOfCurrentMonth, i)
                            )
                        )
                        concatAdapter.notifyItemInserted(0)

                        // set the current month to this sessions month and save its index
                        currentMonth = sessionMonth
                        firstSessionOfCurrentMonth = i
                    }
                }

                // create an adapter for the last subList if is not only the last element,
                // which would be already added to the list
                if(firstSessionOfCurrentMonth != sessions.size - 1) {
                    concatAdapter.addAdapter(
                        0,
                        SessionSummaryAdapter(
                            view.context,
                            false,
                            sessions.subList(firstSessionOfCurrentMonth, sessions.size)
                        )
                    )
                    concatAdapter.notifyItemInserted(0)
                }
            }
        }

        val fab: View = view.findViewById(R.id.fab)
        fab.setOnClickListener {
            // Navigate to the active session screen. Since we defined the <action> in our navigation
            // graph (nav_graph.xml), we can call the NavController to execute it by passing the action's id.
            // The NavController will take care of LayoutInflating, Backstack and Tab presses for us.
            Navigation.findNavController(view)
                .navigate(R.id.action_sessionListFragment_to_activeSessionFragment)
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
                    Category(0, "Die Schöpfung", Color.parseColor("#4caf50"), false, 1),
                    Category(0, "Beethoven Septett", Color.parseColor("#03a9f4"), false, 1),
                    Category(0, "Schostakowitsch 9.", Color.parseColor("#cddc39"), false, 1),
                ).forEach {
                    dao?.insertCategory(it)
                }

                prefs.edit().putBoolean("firstrun", false).apply();
            }
        }
    }
}

