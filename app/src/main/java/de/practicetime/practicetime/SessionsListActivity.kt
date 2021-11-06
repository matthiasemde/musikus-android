package de.practicetime.practicetime

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import de.practicetime.practicetime.entities.Category
import de.practicetime.practicetime.entities.SessionWithSections
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private var dao: PTDao? = null

class SessionsListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sessions_list)

        openDatabase()
        createDatabaseFirstRun()
        fillSessionsListView()

        val fab: View = findViewById(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this, SessionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            applicationContext,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

   private fun fillSessionsListView() {
       // show all sections in listview
       lifecycleScope.launch {
           var sessionsWithSections: List<SessionWithSections>? = dao?.getSessionsWithSections()
           if(sessionsWithSections != null) {
               var listItems = ArrayList<String>()
               for ((session, sections) in sessionsWithSections) {
                   var totalDuration: Int = 0
                   for (section in sections) {
                       totalDuration += section.duration!!
                   }

                   // format date
                   val unixSeconds: Long = sections.first().timestamp
                   // convert seconds to milliseconds
                   val date = Date(unixSeconds * 1000L)
                   // the format of your date
                   val sdf: SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

                   listItems.add("time:" + sdf.format(date) + " | " +
                           totalDuration + "s (" + session.break_duration + "s breaks)" +
                           " | rating: " + session.rating + " | comment: " + session.comment
                   )
               }
               val sessionsList = findViewById<ListView>(R.id.sessionsList)
               var adapter = ArrayAdapter<String>(this@SessionsListActivity, android.R.layout.simple_list_item_1, listItems)
               sessionsList.adapter = adapter
               adapter.notifyDataSetChanged()
           }
       }
   }

    private fun createDatabaseFirstRun() {
        lifecycleScope.launch {
            val prefs = getPreferences(Context.MODE_PRIVATE)

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