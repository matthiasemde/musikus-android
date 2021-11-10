package de.practicetime.practicetime

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.SessionWithSectionsWithCategories
import kotlinx.coroutines.launch

private var dao: PTDao? = null

class SessionListFragment : Fragment(R.layout.fragment_sessions_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        openDatabase()
        createDatabaseFirstRun()

        // initialize adapter and recyclerView for showing category buttons from database
        val sessionWithSectionsWithCategories = ArrayList<SessionWithSectionsWithCategories>()
        val sessionSummaryAdapter = SessionSummaryAdapter(view.context, sessionWithSectionsWithCategories)

        val sessionList = view.findViewById<RecyclerView>(R.id.sessionList)
        sessionList.layoutManager = LinearLayoutManager(view.context)
        sessionList.adapter = sessionSummaryAdapter

        lifecycleScope.launch {
            sessionWithSectionsWithCategories.addAll(dao?.getSessionsWithSectionsWithCategories()!!)

            // notifyDataSetChanged necessary here since all items might have changed
            sessionSummaryAdapter.notifyDataSetChanged()
        }


        val fab: View = view.findViewById(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(view.context, SessionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
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