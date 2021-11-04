package de.practicetime.practicetime

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import de.practicetime.practicetime.entities.PracticeSection
import de.practicetime.practicetime.entities.PracticeSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private var dao: PTDao? = null

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AlertDialog.Builder(this)
            .setMessage("The app will crash on button press if fields are empty!")
            .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener() {
                    dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel()
            })
            .show()

        val db = Room.databaseBuilder(
            applicationContext,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao

        // add section button
        findViewById<Button>(R.id.addSection).setOnClickListener {
            val categoryId = findViewById<EditText>(R.id.categoryId).text.toString().toInt()
            val duration = findViewById<EditText>(R.id.duration).text.toString().toInt()
            val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date()).toLong()

            // id=0 means not assigned, autoGenerate=true will do it for us
            var newSection = PracticeSection(0, 1, categoryId, duration, timeStamp)

            // insert the data asynchronously via lifecycle-aware coroutine
            lifecycleScope.launch {
                dao?.insertSection(newSection)
                fillSectionListView()
            }
        }

        // add session button
        findViewById<Button>(R.id.addSession).setOnClickListener {
            val date = findViewById<EditText>(R.id.date).text.toString().toLong()
            val breakDuration = findViewById<EditText>(R.id.breakDuration).text.toString().toInt()
            val rating = findViewById<EditText>(R.id.rating).text.toString().toInt()
            val comment = findViewById<EditText>(R.id.comment).text.toString()

            // id=0 means not assigned, autoGenerate=true will do it for us
            val newSession = PracticeSession(0, date, breakDuration, rating, comment, 0)

            lifecycleScope.launch {
                dao?.insertSession(newSession)
                fillSessionListView()
            }
        }

        fillSectionListView()
        fillSessionListView()
    }

    private fun fillSectionListView() {
        // show all sections in listview
        lifecycleScope.launch {
            var sections: List<PracticeSection> = dao!!.getAllSections()
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
                        "brk: " + it.break_duration +
                        "r: " + it.rating +
                        "c: " + it.comment
                )
            }
            val sessionsList = findViewById<ListView>(R.id.currentSessions)
            var adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, listItems)
            sessionsList.adapter = adapter
            adapter.notifyDataSetChanged()
        }
    }
}