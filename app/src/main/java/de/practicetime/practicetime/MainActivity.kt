package de.practicetime.practicetime

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.text.Typography.section

private var dao: PTDao? = null

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize Database
        val db = Room.databaseBuilder(
            applicationContext,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao

        // the sectionBuffer will keep track of all the section in the current session
        val sectionBuffer = mutableListOf<PracticeSection>()

        // add section button functionality
        findViewById<Button>(R.id.addSection).setOnClickListener {
            val categoryId = findViewById<EditText>(R.id.categoryId).text.toString()
            val duration = findViewById<EditText>(R.id.duration).text.toString()

            if (categoryId.isEmpty() || duration.isEmpty()) {
                Snackbar.make(it, "Please fill out all Section fields!", Snackbar.LENGTH_SHORT).show()
            } else {

                val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date()).toLong()

                // id=0 means not assigned, autoGenerate=true will do it for us
                val newSection = PracticeSection(
                    0,
                    null,
                    categoryId.toInt(),
                    duration.toInt(),
                    timeStamp
                )

                sectionBuffer.add(newSection)
                lifecycleScope.launch {
                    fillSectionListView(sectionBuffer.toList())
                }
            }
        }

        // add session button functionality
        findViewById<Button>(R.id.addSession).setOnClickListener {
            val date = findViewById<EditText>(R.id.date).text.toString()
            val breakDuration = findViewById<EditText>(R.id.breakDuration).text.toString()
            val rating = findViewById<EditText>(R.id.rating).text.toString()
            val comment = findViewById<EditText>(R.id.comment).text.toString()

            if (date.isEmpty() || breakDuration.isEmpty() || rating.isEmpty() || comment.isEmpty()) {
                Snackbar.make(it, "Please fill out all Session fields!", Snackbar.LENGTH_SHORT).show()
            } else {
                // id=0 means not assigned, autoGenerate=true will do it for us
                val newSession = PracticeSession(
                    0,
                    date.toLong(),
                    breakDuration.toInt(),
                    rating.toInt(),
                    comment,
                    1
                )

                lifecycleScope.launch {
                    val sessionId = dao?.insertSession(newSession)
                    for(section in sectionBuffer) {
                        section.practice_session_id = sessionId?.toInt()
                        dao?.insertSection(section)
                    }
                    sectionBuffer.clear()
                    fillSectionListView(sectionBuffer.toList())
                    fillSessionListView()
                }
            }
        }

        fillSectionListView(sectionBuffer.toList())
        fillSessionListView()
    }

    private fun fillSectionListView(sections: List<PracticeSection>) {
        // show all sections in listview
        lifecycleScope.launch {
            var listItems = ArrayList<String>()
            sections.forEach {
                listItems.add("categ: " + it.category_id + "   |   dur: " + it.duration)
            }
            val sectionsList = findViewById<ListView>(R.id.currentSections)
            var adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, listItems)
            sectionsList.adapter = adapter
            adapter.notifyDataSetChanged()

        }
    }

    private fun fillSessionListView() {
        // show all sections in listview
        lifecycleScope.launch {
            var sessions: List<PracticeSession> = dao!!.getAllSessions()
            var listItems = ArrayList<String>()
            sessions.forEach {
                listItems.add("d: " + it.date +
                        " | brk: " + it.break_duration +
                        " | r: " + it.rating +
                        " | c: " + it.comment
                )
            }
            val sessionsList = findViewById<ListView>(R.id.currentSessions)
            var adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, listItems)
            sessionsList.adapter = adapter
            adapter.notifyDataSetChanged()
        }
    }
}