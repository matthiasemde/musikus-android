package de.practicetime.practicetime

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import de.practicetime.practicetime.entities.SessionWithSectionsWithCategories
import kotlinx.coroutines.launch

private var dao: PTDao? = null

class SessionSummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_summary)

        // load the database
        openDatabase()

        // initialize adapter and recyclerView for showing category buttons from database
        val sessionWithSectionsWithCategories = ArrayList<SessionWithSectionsWithCategories>()
        val sessionSummaryAdapter = SessionSummaryAdapter(this, sessionWithSectionsWithCategories)

        val sessionSummary = findViewById<RecyclerView>(R.id.sessionSummary)
        sessionSummary.layoutManager = LinearLayoutManager(this)
        sessionSummary.adapter = sessionSummaryAdapter

        lifecycleScope.launch {
            sessionWithSectionsWithCategories.add(dao?.getSessionsWithSectionsWithCategories()?.last()!!)

            // notifyDataSetChanged necessary here since all items might have changed
            sessionSummaryAdapter.notifyDataSetChanged()
        }

        val saveSession = findViewById<Button>(R.id.save)
        saveSession.setOnClickListener{goToSessionList()}
    }

    private fun goToSessionList() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            applicationContext,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}